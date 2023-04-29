package frc.robot.subsystems;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile.Constraints;
import edu.wpi.first.math.trajectory.constraint.MaxVelocityConstraint;
import frc.robot.util.SparkMotor;

import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.SparkMaxAbsoluteEncoder;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.SparkMaxAbsoluteEncoder.Type;
import com.revrobotics.SparkMaxPIDController.AccelStrategy;

import static frc.robot.Constants.*;

import java.util.function.Function;

public class Arm extends SubsystemBase {
    private static int STALL_CURRENT_LIMIT_SHOULDER = 30;
    private static int FREE_CURRENT_LIMIT_SHOULDER = 25;
    private static int SECONDARY_CURRENT_LIMIT_SHOULDER = 35;

    private static int STALL_CURRENT_LIMIT_ELBOW = 40;
    private static int FREE_CURRENT_LIMIT_ELBOW = 35;
    private static int SECONDARY_CURRENT_LIMIT_ELBOW = 45;

    private SparkMotor elbowMotor;
    private SparkMotor shoulderMotorRight;
    private SparkMotor shoulderMotorLeft;
    private static SparkMaxAbsoluteEncoder elbowEncoder;
    private static SparkMaxAbsoluteEncoder shoulderEncoder;
    private static SparkMaxPIDController elbowPID;
    private SparkMaxPIDController shoulderPID;
    private double elbowSetpoint;
    private double shoulderSetpoint;
    private ProfiledPIDController profilePID;
    private ProfiledPIDController shoulderProfilePID;
    private Constraints trapezoidalConstraint;

    private double previousVelocity;

    public Arm() {

        elbowMotor = new SparkMotor(7, MotorType.kBrushless);
        trapezoidalConstraint = new Constraints(200, 60);
        profilePID = new ProfiledPIDController(PracticeArmConstants.elbowP, PracticeArmConstants.elbowI, PracticeArmConstants.elbowD, trapezoidalConstraint);
        shoulderProfilePID = new ProfiledPIDController(PracticeArmConstants.shoulderP, PracticeArmConstants.shoulderI, PracticeArmConstants.shoulderD, trapezoidalConstraint);


        elbowMotor.restoreFactoryDefaults();
        elbowMotor.setInverted(true);
        elbowMotor.setIdleMode(IdleMode.kBrake);
        elbowMotor.setSmartCurrentLimit(STALL_CURRENT_LIMIT_ELBOW, FREE_CURRENT_LIMIT_ELBOW);
        elbowMotor.setSecondaryCurrentLimit(SECONDARY_CURRENT_LIMIT_ELBOW);

        elbowPID = elbowMotor.getPIDController();
        elbowEncoder = elbowMotor.getAbsoluteEncoder(Type.kDutyCycle);

        if (PRACTICE_ROBOT) {
            elbowPID.setP(PracticeArmConstants.elbowP);
            elbowPID.setI(PracticeArmConstants.elbowI);
            elbowPID.setD(PracticeArmConstants.elbowD);
            elbowPID.setFF(PracticeArmConstants.elbowFF);
        } else {
            elbowPID.setP(CompArmConstants.elbowP);
            elbowPID.setI(CompArmConstants.elbowI);
            elbowPID.setD(CompArmConstants.elbowD);
        }

        elbowPID.setFeedbackDevice(elbowEncoder);
        elbowPID.setPositionPIDWrappingEnabled(true);
        elbowPID.setPositionPIDWrappingMinInput(0.0);
        elbowPID.setPositionPIDWrappingMaxInput(360);
        elbowPID.setSmartMotionAccelStrategy(AccelStrategy.kTrapezoidal, 0);
        //elbowPID.setSmartMotionAllowedClosedLoopError(0.02, 0);
        elbowPID.setSmartMotionMaxAccel(150, 0);
        elbowPID.setSmartMotionMaxVelocity(360, 0);
        elbowPID.setSmartMotionMinOutputVelocity(0.00, 0);
        elbowEncoder.setPositionConversionFactor(360);
        elbowEncoder.setVelocityConversionFactor(360);
        elbowEncoder.setZeroOffset(ELBOW_ENCODER_OFFSET);
        elbowEncoder.setInverted(true);
        previousVelocity = elbowEncoder.getVelocity();
        elbowMotor.burnFlash();

        shoulderMotorRight = new SparkMotor(8, MotorType.kBrushless);

        shoulderMotorRight.restoreFactoryDefaults();
        shoulderMotorRight.setInverted(true);
        shoulderMotorRight.setIdleMode(IdleMode.kBrake);
        shoulderMotorRight.setSmartCurrentLimit(STALL_CURRENT_LIMIT_SHOULDER, FREE_CURRENT_LIMIT_SHOULDER);
        shoulderMotorRight.setSecondaryCurrentLimit(SECONDARY_CURRENT_LIMIT_SHOULDER);

        shoulderMotorLeft = new SparkMotor(9, MotorType.kBrushless);

        shoulderMotorLeft.restoreFactoryDefaults();
        shoulderMotorLeft.setInverted(false);
        shoulderMotorLeft.setIdleMode(IdleMode.kBrake);
        shoulderMotorLeft.setSmartCurrentLimit(STALL_CURRENT_LIMIT_SHOULDER, FREE_CURRENT_LIMIT_SHOULDER);
        shoulderMotorLeft.setSecondaryCurrentLimit(SECONDARY_CURRENT_LIMIT_SHOULDER);
        shoulderMotorLeft.follow(shoulderMotorRight, true);
        shoulderMotorLeft.burnFlash();

        shoulderPID = shoulderMotorRight.getPIDController();
        shoulderEncoder = shoulderMotorRight.getAbsoluteEncoder(Type.kDutyCycle);
        shoulderEncoder.setInverted(true);
        if (PRACTICE_ROBOT) {
            shoulderPID.setP(PracticeArmConstants.shoulderP);
            shoulderPID.setI(PracticeArmConstants.shoulderI);
            shoulderPID.setD(PracticeArmConstants.shoulderD);
        } else {
            shoulderPID.setP(CompArmConstants.shoulderP);
            shoulderPID.setI(CompArmConstants.shoulderI);
            shoulderPID.setD(CompArmConstants.shoulderD);
        }

        shoulderPID.setFeedbackDevice(shoulderEncoder);
        shoulderPID.setPositionPIDWrappingEnabled(true);
        shoulderPID.setPositionPIDWrappingMinInput(0.0);
        shoulderPID.setPositionPIDWrappingMaxInput(360);
        shoulderEncoder.setPositionConversionFactor(360);
        shoulderEncoder.setZeroOffset(SHOULDER_ENCODER_OFFSET);
        shoulderMotorRight.burnFlash();

    }

    public double getShoulderAngle() {
        double angle = shoulderEncoder.getPosition();
        return angle;
    }

    public double getElbowAngle() {
        double angle = elbowEncoder.getPosition();
        return angle;
    }

    public void setShoulderSetpoint(double setpoint) {
        while (setpoint > 360) {
            setpoint -= 360;
        }
        while (setpoint < 0) {
            setpoint += 360;
        }

        if (setpoint < 0 || setpoint > 280) {

            setpoint = 0;
        } else if (setpoint > 130 && setpoint < 280) {

            setpoint = 130;
        } else {

        }
        if (!violatesFramePerimeter(setpoint, getElbowAngle())) {
            shoulderSetpoint = setpoint;

        } else {
            System.out.println("hit limit");
        }
    }

    public void setElbowSetpoint(double setpoint) {
        while (setpoint > 360) {
            setpoint -= 360;
        }
        while (setpoint < 0) {
            setpoint += 360;
        }

        if (setpoint > 15 && setpoint < 180) {

            setpoint = 14;
        } else if (setpoint < 200 && setpoint > 180) {

            setpoint = 201;

        }
        if (!violatesFramePerimeter(getShoulderAngle(), setpoint)) {
            elbowSetpoint = setpoint;

        } else {
            System.out.println("hit limit");
        }
    }

    public double getElbowSetpoint() {
        return elbowSetpoint;
    }

    public double getShoulderSetpoint() {
        return shoulderSetpoint;
    }

    public Translation2d getArmPosition(double shoulder_Angle, double elbow_Angle) {
        // 105 is when its 6 inches off

        double shoulder_Compliment = 180 - shoulder_Angle;
        double elbowX = Math.cos(Math.toRadians(shoulder_Compliment));
        double elbowY = Math.sin(Math.toRadians(shoulder_Compliment));
        elbowX *= ARM_SHOULDER_LENGTH;
        elbowY *= ARM_SHOULDER_LENGTH;

        double shluckerX = Math.cos(Math.toRadians(elbow_Angle + shoulder_Compliment));
        double shluckerY = Math.sin(Math.toRadians(elbow_Angle + shoulder_Compliment));
        shluckerX *= ARM_ELBOW_LENGTH;
        shluckerY *= ARM_ELBOW_LENGTH;

        Translation2d output = new Translation2d(elbowX + shluckerX, elbowY + shluckerY);
        return output;
    }

    public boolean violatesFramePerimeter(double shoulder_Angle, double elbow_Angle) {
        double currentExtension = getArmPosition(getShoulderAngle(), getElbowAngle()).getX();
        double desiredExtension = getArmPosition(shoulder_Angle, elbow_Angle).getX();
        return shoulder_Angle > 105 && !(desiredExtension < currentExtension || desiredExtension < 45);

    }

    @Override
    public void periodic() {
        if (DEBUG) {

            double acceleration = (elbowEncoder.getVelocity() - previousVelocity );


            SmartDashboard.putNumber("shoulder angle", getShoulderAngle());
            SmartDashboard.putNumber("shoulder setpoint", shoulderSetpoint);
            SmartDashboard.putNumber("shoulder left output", shoulderMotorRight.getAppliedOutput());
            SmartDashboard.putNumber("shoulder right output", shoulderMotorLeft.getAppliedOutput());
            SmartDashboard.putNumber("shoulder X", getArmPosition(getShoulderAngle(), getElbowAngle()).getX());
            SmartDashboard.putNumber("shoulder Y", getArmPosition(getShoulderAngle(), getElbowAngle()).getY());

            SmartDashboard.putNumber("elbow angle", getElbowAngle());
            SmartDashboard.putNumber("elbow setpoint", elbowSetpoint);
            SmartDashboard.putNumber("elbow outut", elbowMotor.getAppliedOutput());

            SmartDashboard.putNumber("Elbow velocity", elbowEncoder.getVelocity());
            SmartDashboard.putNumber("Shoulder velocity", shoulderEncoder.getVelocity());
            SmartDashboard.putNumber("Elbow Acceleration", acceleration);

            
            previousVelocity = elbowEncoder.getVelocity();

        }

        shoulderPID.setReference(shoulderSetpoint, ControlType.kPosition);
        elbowMotor.setVoltage(
            profilePID.calculate(getElbowAngle(), elbowSetpoint));
        shoulderMotorRight.setVoltage(
            shoulderProfilePID.calculate(getShoulderAngle(), shoulderSetpoint));
    }




}
