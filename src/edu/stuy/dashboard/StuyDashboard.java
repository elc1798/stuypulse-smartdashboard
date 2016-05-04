package edu.stuy.dashboard;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import edu.stuy.dashboard.gui.StuyDashboardFrame;
import edu.stuy.dashboard.gui.StuyDashboardPrefs;
import edu.wpi.first.smartdashboard.ArgParser;
import edu.wpi.first.smartdashboard.extensions.FileSniffer;
import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.smartdashboard.robot.Robot;

public class StuyDashboard {
    private static boolean inCompetition = false;
    private static StuyDashboardFrame frame;

    public static boolean inCompetition() {
        return inCompetition;
    }

    public static void main(String[] args) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception localException) {
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(2);
        }

        ProgressMonitor monitor = new ProgressMonitor(null, "Loading SmartDashboard", "Initializing internal code...", 0, 1000);

        FileSniffer.findExtensions(monitor, 0, 490);

        ArgParser argParser = new ArgParser(args, true, true, new String[] {
            "ip"
        });
        inCompetition = argParser.hasFlag("competition");

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new StuyDashboardFrame(StuyDashboard.inCompetition);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(2);
        }

        if (argParser.hasValue("ip")) {
            monitor.setProgress(650);
            monitor.setNote("Connecting to robot at: " + argParser.getValue("ip"));
            Robot.setHost(argParser.getValue("ip"));
            System.out.println("IP: " + argParser.getValue("ip"));
        } else {
            monitor.setProgress(600);
            monitor.setNote("Getting Team Number");
            IntegerProperty teamProp = frame.getPrefs().team;
            int teamNumber = teamProp.getValue().intValue();

            while (teamNumber <= 0)
                try {
                    String input = JOptionPane.showInputDialog("Input Team Number");
                    if (input == null) {
                        teamNumber = 0;
                        break;
                    }
                    teamNumber = Integer.parseInt(input);
                } catch (Exception localException1) {
                }
            monitor.setProgress(650);
            monitor.setNote("Connecting to robot of team: " + teamNumber);
            teamProp.setValue(Integer.valueOf(teamNumber));
            Robot.setUseMDNS(((Boolean) StuyDashboardPrefs.getInstance().usemDNS.getValue()).booleanValue());
            Robot.setTeam(teamNumber);
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        StuyDashboard.frame.pack();
                        StuyDashboard.frame.setVisible(true);

                        monitor.setProgress(750);
                        monitor.setNote("Loading From Save");

                        File file = new File((String) StuyDashboard.frame.getPrefs().saveFile.getValue());
                        if (file.exists()) {
                            StuyDashboard.frame.load(file.getPath());
                        }

                        monitor.setProgress(1000);
                    } catch (Exception e) {
                        e.printStackTrace();

                        System.exit(1);
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(2);
        }
    }
}
