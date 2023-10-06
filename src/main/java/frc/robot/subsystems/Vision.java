package frc.robot.subsystems;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.photonvision.EstimatedRobotPose;
import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;
import org.photonvision.targeting.PhotonPipelineResult;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Vision extends SubsystemBase {
    PhotonCamera camera;
    PhotonPoseEstimator poseEstimator;
    EstimatedRobotPose lastResult;
    BiConsumer<Pose2d, Double> consumer;

    public Vision(BiConsumer<Pose2d, Double> consumer) {
        this.consumer = consumer;
        lastResult = null;

        camera = new PhotonCamera("MainC");

        AprilTagFieldLayout fieldLayout = null;
        try {
            fieldLayout = AprilTagFieldLayout.loadFromResource(AprilTagFields.k2023ChargedUp.m_resourceFile);
        } catch (IOException e) {
            System.out.println("Failed to load AprilTag layout!");
            e.printStackTrace();
            return;
        }

        poseEstimator = new PhotonPoseEstimator(
                fieldLayout,
                PoseStrategy.MULTI_TAG_PNP,
                camera,
                new Transform3d(new Translation3d(
                        Units.inchesToMeters(0),
                        Units.inchesToMeters(0),
                        Units.inchesToMeters(0)),
                        new Rotation3d()));
        poseEstimator.setMultiTagFallbackStrategy(PoseStrategy.LOWEST_AMBIGUITY);
    }

    @Override
    public void periodic() {
        boolean connected = camera.isConnected();
        SmartDashboard.putBoolean("Vision Connected", connected);
        if (!connected)
            return;

        PhotonPipelineResult result = camera.getLatestResult();
        if (!result.hasTargets())
            return;

        Optional<EstimatedRobotPose> poseResult = poseEstimator.update();
        if (!poseResult.isPresent())
            return;

        lastResult = poseResult.get();
        if (consumer == null)
            return;

        consumer.accept(lastResult.estimatedPose.toPose2d(), lastResult.timestampSeconds);
    }

    public Pose3d getLastResult() {
        return lastResult.estimatedPose;
    }
}
