package Model;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Based on the class MultiPolygonApprox by Troels Bjerre Lund.
 */
public class MultiPolygonApprox extends PolygonApprox {
    private static final long serialVersionUID = 16052016L;
    private byte[] pointtypes;

    public MultiPolygonApprox(List<List<Point2D>> rel) {
        int npoints = 0;
        for (List<?> l : rel) npoints += l.size();
        coords = new float[npoints<<1];
        pointtypes = new byte[npoints];
        Arrays.fill(pointtypes, (byte) PathIterator.SEG_LINETO);
        int coord = 0;
        int point = 0;
        for (List<? extends Point2D> l : rel) {
            pointtypes[point] = (byte) PathIterator.SEG_MOVETO;
            point += l.size();
            for (Point2D p : l) {
                coords[coord++] = (float)p.getX();
                coords[coord++] = (float)p.getY();
            }
        }
        if(npoints > 0) {
            init();
        }
    }

    /**
     * Checks if this is a MultiPolygonApprox instead of using instanceOf
     * @return Always true. This is used as a faster alternative to instanceOf
     */
    public boolean isMultiPA(){
        return true;
    }


    public double distTo(Point2D p) {
        double dist = Double.MAX_VALUE;
        double px = p.getX();
        double py = p.getY();
        for (int i = 2 ; i < coords.length ; i += 2) {
            if (pointtypes[i >> i] != PathIterator.SEG_MOVETO)
                dist = Math.min(dist, Line2D.ptSegDist(coords[i-2], coords[i-1], coords[i], coords[i+1], px, py));
        }
        return dist;
    }

    public PathIterator getPathIterator(AffineTransform at, float pixelsq) {
        return new MultiPolygonApproxIterator(at, pixelsq);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new MultiPolygonApproxIterator(at, (float) (flatness * flatness));
    }

    private class MultiPolygonApproxIterator extends PolygonApproxIterator {
        public MultiPolygonApproxIterator(AffineTransform _at, float _pixelsq) {
            super(_at, _pixelsq);
        }

        public void next() {
            float fx = coords[index];
            float fy = coords[index+1];
            index += 2;
            while (index < coords.length - 2 && pointtypes[(index >> 1) + 1] == PathIterator.SEG_LINETO &&
                    distSq(fx, fy, coords[index], coords[index+1]) < approx) index += 2;
        }

        public int currentSegment(float[] c) {
            if (isDone()) {
                throw new NoSuchElementException("poly approx iterator out of bounds");
            }
            c[0] = coords[index];
            c[1] = coords[index+1];
            if (at != null) {
                at.transform(c, 0, c, 0, 1);
            }
            return pointtypes[index >> 1];
        }

        public int currentSegment(double[] coords) {
            throw new UnsupportedOperationException("Unexpected call to PolygonApprox.contains(Rectangle2D)");
        }
    }
}