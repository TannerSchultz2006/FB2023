package frc.robot.commands;

import frc.robot.subsystems.Arm;
import frc.robot.subsystems.Arm.ArmConstants;

public class ArmHighCommand extends ArmPositionCommand {
    public ArmHighCommand(Arm arm) {
        super(arm, ArmConstants.ELBOW_HIGH, ArmConstants.SHOULDER_HIGH);
    }
}
