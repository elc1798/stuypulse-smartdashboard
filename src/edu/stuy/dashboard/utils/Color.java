package edu.stuy.dashboard.utils;


@SuppressWarnings("serial")
public class Color extends java.awt.Color {

    public Color(int r, int g, int b) {
        super(r, g, b);
        // TODO Auto-generated constructor stub
    }

    public edu.stuy.dashboard.utils.Color inverted() {
        int invR = (getRed() & 0xFF) ^ 0xFF;
        int invG = (getGreen() & 0xFF) ^ 0xFF;
        int invB = (getBlue() & 0xFF) ^ 0xFF;
        return new edu.stuy.dashboard.utils.Color(invR, invG, invB);
    }

}
