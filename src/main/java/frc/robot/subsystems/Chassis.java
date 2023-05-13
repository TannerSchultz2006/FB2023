package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.RamseteCommand;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.math.controller.RamseteController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.estimator.DifferentialDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.math.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import edu.wpi.first.math.trajectory.constraint.DifferentialDriveVoltageConstraint;
import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.wpilibj.DriverStation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;

public class Chassis extends SubsystemBase {
    private static class Constants {
        // Driving
        public static final int RIGHT_FRONT_CAN_ID = 15;
        public static final int RIGHT_BACK_CAN_ID = 17;
        public static final int LEFT_FRONT_CAN_ID = 16;
        public static final int LEFT_BACK_CAN_ID = 18;

        public static final double TRACK_WIDTH = 0.96679; // Meters
        public static final double MAX_VELOCITY = 5.0; // Meters per second
        public static final double MAX_ACCELERATION = 5.0; // Meters per second squared
        public static final double SLOW_VELOCITY = 1.0; // Meters per second
        public static final double MAX_ANGULAR_VELOCITY = 8.0; // Radians per second
        public static final double MAX_ANGULAR_ACCELERATION = 8.0; // Radians per second squared
        public static final double SLOW_ANGULAR_VELOCITY = 2.0; // Radians per second
        public static final double S = 0.15473;
        public static final double V = 2.3007;
        public static final double A = 0.22029;

        // Trajectories
        public static final double MAX_VOLTAGE = 10.0;

        // Charge Station
        public static final double LEVEL_TOLERANCE = 2.0; // degrees
        public static final double ON_TOLERANCE = 10.0; // degrees
        public static final double PITCH_VELOCITY_MAX = 0.2; // degrees per cycle
    }

    // Driving
    private final ChassisSide left, right;
    private final SimpleMotorFeedforward feedforward;
    private final DifferentialDriveKinematics kinematics;

    // Localization
    private AHRS navX;
    private final DifferentialDrivePoseEstimator poseEstimator;
    private final Field2d field;

    // Trajectories
    private final DifferentialDriveVoltageConstraint constraint;
    private final TrajectoryConfig config;
    private final RamseteController controller;

    // Charge Station
    private double lastPitch = 0;
    private double pitchVelocity = 0;

    public Chassis(DataLog log) {
        feedforward = new SimpleMotorFeedforward(Constants.S, Constants.V, Constants.A);
        kinematics = new DifferentialDriveKinematics(Constants.TRACK_WIDTH);

        left = new ChassisSide(Constants.LEFT_FRONT_CAN_ID, Constants.LEFT_BACK_CAN_ID, false, feedforward, log, "Left");
        right = new ChassisSide(Constants.RIGHT_FRONT_CAN_ID, Constants.RIGHT_BACK_CAN_ID, true, feedforward, log, "Right");
        try {
            navX = new AHRS(SPI.Port.kMXP);
        } catch (RuntimeException ex) {
            DriverStation.reportError(ex.getMessage(), true);
        }

        poseEstimator = new DifferentialDrivePoseEstimator(kinematics,
                navX.getRotation2d(), 0, 0, new Pose2d());
        field = new Field2d();
        addChild("Field", field);

        constraint = new DifferentialDriveVoltageConstraint(feedforward, kinematics, Constants.MAX_VOLTAGE);
        config = new TrajectoryConfig(Constants.MAX_VELOCITY, Constants.MAX_ACCELERATION);
        config.setKinematics(kinematics);
        config.addConstraint(constraint);
        controller = new RamseteController(); // Uses default constants
    }

    @Override
    public void periodic() {
        double leftDistance = left.update();
        double rightDistance = right.update();

        poseEstimator.update(navX.getRotation2d(), leftDistance, rightDistance);
        field.setRobotPose(getPose());

        double currentPitch = getPitchDegrees();
        pitchVelocity = currentPitch - lastPitch;
        lastPitch = currentPitch;
    }

    /****************** DRIVING ******************/
    public void drive(ChassisSpeeds chassisSpeeds) {
        tankDrive(kinematics.toWheelSpeeds(chassisSpeeds));
    }

    private void tankDrive(DifferentialDriveWheelSpeeds wheelSpeeds) {
        tankDrive(wheelSpeeds.leftMetersPerSecond, wheelSpeeds.rightMetersPerSecond);
    }

    private void tankDrive(double leftSpeed, double rightSpeed) {
        left.setSetpoint(leftSpeed);
        right.setSetpoint(rightSpeed);
    }

    public void setBrakeMode(boolean brakeMode) {
        left.setBrakeMode(brakeMode);
        right.setBrakeMode(brakeMode);
    }

    /****************** LOCALIZATION ******************/
    public void visionPose(Pose2d pose, double timestampSeconds) {
        poseEstimator.addVisionMeasurement(pose, timestampSeconds);
    }

    public Pose2d getPose() {
        return poseEstimator.getEstimatedPosition();
    }

    /****************** TRAJECTORIES ******************/
    private Trajectory generateTrajectory(Pose2d start, Pose2d end, boolean reversed) {
        return generateTrajectory(start, new ArrayList<>(), end, reversed);
    }

    private Trajectory generateTrajectory(Pose2d start, List<Translation2d> interior, Pose2d end, boolean reversed) {
        config.setReversed(reversed);
        return TrajectoryGenerator.generateTrajectory(start, interior, end, config);
    }

    private RamseteCommand generateRamseteCommand(Trajectory trajectory) {
        return new RamseteCommand(
                trajectory,
                this::getPose,
                controller,
                kinematics,
                this::tankDrive,
                this);
    }

    public Command driveDistance(double distance) {
        Trajectory trajectory = generateTrajectory(
                new Pose2d(0, 0, new Rotation2d()),
                new Pose2d(1.0, 0, new Rotation2d()),
                distance < 0);
        return generateRamseteCommand(trajectory);
    }

    /****************** CHARGE STATION ******************/
    public double getPitchDegrees() {
        return navX.getPitch();
    }

    public boolean isLevel() {
        return Math.abs(getPitchDegrees()) < Constants.LEVEL_TOLERANCE;
    }

    public boolean isOnChargeStation() {
        return Math.abs(getPitchDegrees()) > Constants.ON_TOLERANCE;
    }

    private double getPitchVelocityDegrees() {
        return pitchVelocity;
    }

    public boolean isNotPitching() {
        return Math.abs(getPitchVelocityDegrees()) < Constants.PITCH_VELOCITY_MAX;
    }

    /****************** ROTATION ******************/
    public double getYawDegrees() {
        return navX.getYaw();
    }

    /****************** COMMANDS ******************/
    public Command defaultCommand(Supplier<Double> forwardSupplier, Supplier<Double> rotationSupplier,
            Supplier<Boolean> slowModeSupplier) {
        return new RunCommand(() -> {
            double forward = forwardSupplier.get() * -1.0;
            double rotation = rotationSupplier.get() * -1.0;

            if (slowModeSupplier.get()) {
                forward *= Constants.SLOW_VELOCITY;
                rotation *= Constants.SLOW_ANGULAR_VELOCITY;
            } else {
                forward *= Constants.MAX_VELOCITY;
                rotation *= Constants.MAX_ANGULAR_VELOCITY;
            }

            drive(new ChassisSpeeds(forward, 0, rotation));
        }, this);
    }
}
