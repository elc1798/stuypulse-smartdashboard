package edu.stuy.dashboard.workarounds;

import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

import edu.stuy.dashboard.gui.StuyDashboardFrame;
import edu.wpi.first.smartdashboard.robot.Robot;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;

public class LogToCSV
  implements ITableListener
{
  private static final String s_lineSeparator = System.getProperty("line.separator");
  private long m_startTime;
  private FileWriter m_fw;
  private final StuyDashboardFrame frame;

  public LogToCSV(StuyDashboardFrame frame)
  {
    this.frame = frame;
  }

  public void start(String path)
  {
    if (m_fw == null)
      try {
        m_startTime = System.currentTimeMillis();
        m_fw = new FileWriter(path);
        m_fw.write("Time (ms),Name,Value" + s_lineSeparator);
        m_fw.flush();
        Robot.getTable().addTableListenerEx(this, 23);
      } catch (IOException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, "An error occurred when attempting to open the output CSV file for writing. Please check the file path preference.", "Unable to Open CSV File", 0);

        frame.getPrefs().logToCSV.setValue(Boolean.valueOf(false));
      }
  }

  public void stop()
  {
    if (m_fw == null) {
      return;
    }
    try
    {
      m_fw.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    Robot.getTable().removeTableListener(this);
    m_fw = null;
  }

  public void valueChanged(ITable source, String key, Object value, boolean isNew)
  {
    if ((!(value instanceof ITable)) && (m_fw != null))
      try {
        long timeStamp = System.currentTimeMillis() - m_startTime;
        m_fw.write(timeStamp + "," + "\"" + key + "\"," + "\"" + value + "\"" + s_lineSeparator);
        m_fw.flush();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
  }
}