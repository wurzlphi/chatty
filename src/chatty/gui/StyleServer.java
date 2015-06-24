
package chatty.gui;

import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import javax.swing.text.MutableAttributeSet;

/**
 * Provide style information to other objects.
 * 
 * @author tduva
 */
public interface StyleServer {
    public Color getColor(String type);
    public MutableAttributeSet getStyle();
    public MutableAttributeSet getStyle(String type);
    public Font getFont();
    public SimpleDateFormat getTimestampFormat();
}
