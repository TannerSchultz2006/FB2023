// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.Schlucker;

public class ShluckerCommand extends CommandBase {
  public ItemHeld _item_held;
  private final Schlucker m_schlucker;
  private double speed;

  public ShluckerCommand(double speed, Schlucker subsystem) {

    m_schlucker = subsystem;
    this.speed = speed;
    addRequirements(m_schlucker);

  }

  @Override
  public void initialize() {
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {

    m_schlucker.setShluckerSpeed(speed);
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    m_schlucker.setShluckerSpeed(0);
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }

  enum ItemHeld {
    CONE,
    CUBE,
    NONE  
  }

  public void intakeCube() {
    _item_held = ItemHeld.CUBE;
  }
  
  public void intakeCone() {
    _item_held = ItemHeld.CONE;
  }
  
  public void eject() {
    _item_held = ItemHeld.NONE;
  }

}
