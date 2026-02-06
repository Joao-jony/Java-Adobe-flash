import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

class AnimationController {
    
    public static double linearInterpolation(double start, double end, double t) {
        return start + (end - start) * t;
    }
    
    public static Color interpolateColor(Color start, Color end, double t) {
        int r = (int) linearInterpolation(start.getRed(), end.getRed(), t);
        int g = (int) linearInterpolation(start.getGreen(), end.getGreen(), t);
        int b = (int) linearInterpolation(start.getBlue(), end.getBlue(), t);
        int a = (int) linearInterpolation(start.getAlpha(), end.getAlpha(), t);
        
        return new Color(
            Math.min(255, Math.max(0, r)),
            Math.min(255, Math.max(0, g)),
            Math.min(255, Math.max(0, b)),
            Math.min(255, Math.max(0, a))
        );
    }
    
    // Encontrar keyframes adjacentes para um frame específico
    public static Keyframe[] findAdjacentKeyframes(ArrayList<Keyframe> keyframes, int frame) {
        if (keyframes == null || keyframes.isEmpty()) {
            return null;
        }
        
        // Ordenar keyframes por frame
        keyframes.sort((k1, k2) -> Integer.compare(k1.getFrame(), k2.getFrame()));
        
        Keyframe prev = null;
        Keyframe next = null;
        
        for (Keyframe kf : keyframes) {
            if (kf.getFrame() <= frame) {
                prev = kf;
            }
            if (kf.getFrame() >= frame) {
                next = kf;
                break;
            }
        }
        
        // Se não encontrou next, usar o último keyframe
        if (next == null && prev != null) {
            next = prev;
        }
        // Se não encontrou prev, usar o primeiro keyframe
        if (prev == null && next != null) {
            prev = next;
        }
        
        return new Keyframe[]{prev, next};
    }
    
    // Calcular transformação interpolada para um frame específico
    public static AffineTransform getInterpolatedTransform(ArrayList<Keyframe> keyframes, int frame) {
        if (keyframes == null || keyframes.isEmpty()) {
            return new AffineTransform(); // Transformação identidade
        }
        
        Keyframe[] adjacent = findAdjacentKeyframes(keyframes, frame);
        
        if (adjacent == null || adjacent[0] == null || adjacent[1] == null) {
            return new AffineTransform(); // Transformação identidade
        }
        
        Keyframe prev = adjacent[0];
        Keyframe next = adjacent[1];
        
        if (prev.getFrame() == next.getFrame() || prev.getFrame() == frame) {
            return new AffineTransform(prev.getTransform());
        }
        
        // Calcular fator de interpolação
        double t = (double)(frame - prev.getFrame()) / (next.getFrame() - prev.getFrame());
        t = Math.max(0, Math.min(1, t)); // Clamp entre 0 e 1
        
        // Obter matrizes de transformação
        double[] prevMatrix = new double[6];
        double[] nextMatrix = new double[6];
        prev.getTransform().getMatrix(prevMatrix);
        next.getTransform().getMatrix(nextMatrix);
        
        // Interpolar cada componente da matriz
        double[] interpolatedMatrix = new double[6];
        for (int i = 0; i < 6; i++) {
            interpolatedMatrix[i] = linearInterpolation(prevMatrix[i], nextMatrix[i], t);
        }
        
        return new AffineTransform(interpolatedMatrix);
    }
    
    // Obter cor interpolada para um frame específico
    public static Color getInterpolatedColor(ArrayList<Keyframe> keyframes, int frame) {
        if (keyframes == null || keyframes.isEmpty()) {
            return Color.BLACK;
        }
        
        Keyframe[] adjacent = findAdjacentKeyframes(keyframes, frame);
        
        if (adjacent == null || adjacent[0] == null || adjacent[1] == null) {
            return Color.BLACK;
        }
        
        Keyframe prev = adjacent[0];
        Keyframe next = adjacent[1];
        
        if (prev.getFrame() == next.getFrame() || prev.getFrame() == frame) {
            return prev.getColor();
        }
        
        double t = (double)(frame - prev.getFrame()) / (next.getFrame() - prev.getFrame());
        t = Math.max(0, Math.min(1, t));
        
        return interpolateColor(prev.getColor(), next.getColor(), t);
    }
    
    // Obter largura do traço interpolada
    public static float getInterpolatedStrokeWidth(ArrayList<Keyframe> keyframes, int frame) {
        if (keyframes == null || keyframes.isEmpty()) {
            return 2.0f;
        }
        
        Keyframe[] adjacent = findAdjacentKeyframes(keyframes, frame);
        
        if (adjacent == null || adjacent[0] == null || adjacent[1] == null) {
            return 2.0f;
        }
        
        Keyframe prev = adjacent[0];
        Keyframe next = adjacent[1];
        
        if (prev.getFrame() == next.getFrame() || prev.getFrame() == frame) {
            return prev.getStrokeWidth();
        }
        
        double t = (double)(frame - prev.getFrame()) / (next.getFrame() - prev.getFrame());
        t = Math.max(0, Math.min(1, t));
        
        return (float) linearInterpolation(prev.getStrokeWidth(), next.getStrokeWidth(), t);
    }
    
    // Verificar se há animação na forma
    public static boolean hasAnimation(ShapeObject shape) {
        return shape != null && shape.getKeyframes().size() > 1;
    }
    
    // Obter a duração da animação da forma
    public static int getAnimationDuration(ShapeObject shape) {
        if (shape == null || shape.getKeyframes().isEmpty()) {
            return 0;
        }
        
        int minFrame = Integer.MAX_VALUE;
        int maxFrame = Integer.MIN_VALUE;
        
        for (Keyframe kf : shape.getKeyframes()) {
            minFrame = Math.min(minFrame, kf.getFrame());
            maxFrame = Math.max(maxFrame, kf.getFrame());
        }
        
        return maxFrame - minFrame;
    }
    
    // Obter frame inicial da animação
    public static int getStartFrame(ShapeObject shape) {
        if (shape == null || shape.getKeyframes().isEmpty()) {
            return 0;
        }
        
        int minFrame = Integer.MAX_VALUE;
        for (Keyframe kf : shape.getKeyframes()) {
            minFrame = Math.min(minFrame, kf.getFrame());
        }
        return minFrame;
    }
    
    // Obter frame final da animação
    public static int getEndFrame(ShapeObject shape) {
        if (shape == null || shape.getKeyframes().isEmpty()) {
            return 0;
        }
        
        int maxFrame = Integer.MIN_VALUE;
        for (Keyframe kf : shape.getKeyframes()) {
            maxFrame = Math.max(maxFrame, kf.getFrame());
        }
        return maxFrame;
    }
}