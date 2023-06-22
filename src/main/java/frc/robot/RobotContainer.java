package frc.robot;

import frc.robot.subsystems.Arm;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.Schlucker;
import frc.robot.subsystems.SchluckerBag;
import frc.robot.subsystems.SchluckerNeo550;
import frc.robot.util.GamePiece;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.util.datalog.StringLogEntry;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.button.CommandJoystick;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;

public class RobotContainer {
    public class Constants {
        public static final int JOYSTICK_1_PORT = 0;
        public static final int JOYSTICK_2_PORT = 1;
        public static final int CONTROLLER_PORT = 2;
    }

    private final DriveSubsystem drive;
    private final Arm arm;
    private final Schlucker schlucker;

    private final CommandJoystick joystick_1;
    private final CommandJoystick joystick_2;
    private final CommandXboxController controller;
    private final SendableChooser<Command> autoSelector;
    private final DataLog log;
    private final StringLogEntry autoLog;

    public RobotContainer() {
        DataLogManager.start();
        log = DataLogManager.getLog();

        drive = new DriveSubsystem();
        arm = new Arm(log);
        schlucker = new SchluckerBag(log); // new SchluckerNeo550();

        joystick_1 = new CommandJoystick(Constants.JOYSTICK_1_PORT);
        joystick_2 = new CommandJoystick(Constants.JOYSTICK_2_PORT);
        controller = new CommandXboxController(Constants.CONTROLLER_PORT);
        DriverStation.startDataLog(log);

        autoSelector = new SendableChooser<>();
        autoLog = new StringLogEntry(log, "/Auto/Command");

        configureButtonBindings();

        displayGitInfo(log);
    }

    private void configureButtonBindings() {
        // TODO: Improve this to be a command factory
        drive.setDefaultCommand(new RunCommand(() -> {
            drive.drive(joystick_1.getX(), joystick_1.getY(), joystick_2.getX(), false, true);
        }, drive));

        arm.setDefaultCommand(arm.defaultCommand(controller::getLeftY, controller::getRightY));

        // Arm target point commands
        controller.povUp().onTrue(arm.substation());
        controller.povRight()
                .onTrue(arm.mid().andThen(schlucker.eject()))
                .onFalse(schlucker.stop());
        controller.povDown().onTrue(arm.groundCone());
        controller.povLeft()
                .onTrue(arm.high().andThen(schlucker.eject()))
                .onFalse(schlucker.stop());
        controller.leftBumper().onTrue(arm.groundCube());
        controller.b().onTrue(arm.stow());

        // Schlucker commands
        controller.a()
                .onTrue(schlucker.intakeCone())
                .onFalse(schlucker.hold());
        controller.x()
                .onTrue(schlucker.intakeCube())
                .onFalse(schlucker.hold());
        controller.y()
                .onTrue(schlucker.eject())
                .onFalse(schlucker.stop());

        joystick_1.button(3).onTrue(schlucker.wantCone());
        joystick_1.button(4).onTrue(schlucker.wantCube());
    }

    public Command getAutonomousCommand() {
        Command auto = autoSelector.getSelected();
        autoLog.append(auto.getName());
        return auto;
    }

    private static void displayGitInfo(DataLog log) {
        final NetworkTable table = NetworkTableInstance.getDefault().getTable("Build Info");
        table.getEntry("Branch Name").setString(BuildConstants.GIT_BRANCH);
        table.getEntry("Commit Hash (Short)").setString(BuildConstants.GIT_SHA.substring(0, 8));
        table.getEntry("Commit Hash (Full)").setString(BuildConstants.GIT_SHA);
        table.getEntry("Dirty").setBoolean(BuildConstants.DIRTY == 1);
        table.getEntry("Build Date & Time").setString(BuildConstants.BUILD_DATE);

        log.setMetadata(0, BuildConstants.GIT_BRANCH);
        log.setMetadata(1, BuildConstants.GIT_SHA);
        log.setMetadata(2, String.valueOf(BuildConstants.DIRTY == 1));
        log.setMetadata(3, BuildConstants.BUILD_DATE);
    }
}
