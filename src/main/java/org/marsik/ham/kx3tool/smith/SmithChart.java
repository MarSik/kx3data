package org.marsik.ham.kx3tool.smith;

import java.util.Collection;

import lombok.Value;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.line.Line2D;
import math.geom2d.line.LinearShape2D;
import org.apache.commons.math3.complex.Complex;

@Value
public class SmithChart {
    Complex z0;

    public SmithChart(Complex z0) {
        this.z0 = z0;
    }

    public SmithChart() {
        this(Complex.valueOf(50.0, 0.0));
    }

    /**
     * Convert impedance to normalized cartesian coordinates centered at Z0.
     *
     * @param z impedance to plot
     * @return cartesian coordinates of the impedance in the Smith chart plot
     */
    public Complex plot(Complex z) {
        final Complex norm = z.divide(z0);

        // constant impedance
        // double circleRadius = 1 / (1 + norm.real);
        // center: X = 1 - circleRadius; Y = 0

        // constant reactance
        // double circleRadius = 1 / norm.imag;
        // center: X = 1, Y = circleRadius

        // compute circle intersections
        double impedanceRadius = 1 / (1 + norm.getReal());
        Circle2D impedance = new Circle2D(1 - impedanceRadius, 0, impedanceRadius);

        final Collection<Point2D> intersections;

        if (norm.getImaginary() != 0) {
            double reactanceRadius = 1 / norm.getImaginary();
            Circle2D reactance = new Circle2D(1, reactanceRadius, Math.abs(reactanceRadius));
            intersections = impedance.intersections(reactance);
        } else {
            Line2D line = new Line2D(-1, 0, 1, 0);
            intersections = impedance.intersections(line);
        }

        return intersections.stream()
                .filter(i -> i.x() < 1)
                .map(i -> Complex.valueOf(i.x(), i.y()))
                .findFirst()
                .orElse(Complex.valueOf(-1, -1));
    }

    private double omega(double frequency) {
        return frequency * 2 * Math.PI;
    }

    protected Complex resistanceToImpedance(double R) {
        return Complex.valueOf(R);
    }

    protected Complex inductanceToImpedance(double frequency, double L) {
        return Complex.valueOf(0.0, omega(frequency) * L);
    }

    protected Complex capacitanceToImpedance(double frequency, double C) {
        return Complex.valueOf(0.0, -1/(omega(frequency) * C));
    }
}
