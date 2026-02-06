import java.awt.*;
import java.awt.geom.*;
import java.io.Serializable;
import java.util.ArrayList;

// Classe abstrata base para todas as formas
abstract class ShapeObject implements Serializable {
    protected ArrayList<Keyframe> keyframes;
    protected Color baseColor;
    protected float baseStrokeWidth;
    protected boolean selected;
    protected String type;
    protected boolean visible = true;
    
    public ShapeObject(Color color, float strokeWidth, String type) {
        this.baseColor = color;
        this.baseStrokeWidth = strokeWidth;
        this.type = type;
        this.keyframes = new ArrayList<>();
        this.selected = false;
        
        // Adicionar um keyframe inicial no frame 0
        addKeyframe(0, color, strokeWidth);
    }
    
    // Métodos abstratos
    public abstract boolean contains(Point point, int frame);
    public abstract void updateShape(Point point);
    public abstract int getBaseX();
    public abstract int getBaseY();
    public abstract int getBaseWidth();
    public abstract int getBaseHeight();
    
    // Métodos concretos
    public void addKeyframe(int frame) {
        addKeyframe(frame, baseColor, baseStrokeWidth);
    }
    
    public void addKeyframe(int frame, Color color, float strokeWidth) {
        // Verificar se já existe keyframe neste frame
        for (Keyframe kf : keyframes) {
            if (kf.getFrame() == frame) {
                // Atualizar keyframe existente
                kf.setColor(color);
                kf.setStrokeWidth(strokeWidth);
                updateKeyframeTransform(kf, frame);
                return;
            }
        }
        
        // Criar novo keyframe
        Keyframe keyframe = new Keyframe(frame);
        keyframe.setColor(color);
        keyframe.setStrokeWidth(strokeWidth);
        
        // Configurar transformação base
        updateKeyframeTransform(keyframe, frame);
        
        keyframes.add(keyframe);
        sortKeyframes();
    }
    
    // Atualizar transformação do keyframe
    protected void updateKeyframeTransform(Keyframe keyframe, int frame) {
        AffineTransform transform = new AffineTransform();
        
        // Para formas geométricas, usar posição base
        if (!(this instanceof PencilShape)) {
            int x = getBaseX();
            int y = getBaseY();
            transform.translate(x, y);
        }
        
        keyframe.setTransform(transform);
    }
    
    // Ordenar keyframes por frame
    private void sortKeyframes() {
        keyframes.sort((k1, k2) -> Integer.compare(k1.getFrame(), k2.getFrame()));
    }
    
    public ArrayList<Keyframe> getKeyframes() {
        return keyframes;
    }
    
    // Desenhar forma com interpolação automática
    public void draw(Graphics2D g2d, int frame) {
        if (!visible) return;
        
        // Salvar transformação original
        AffineTransform originalTransform = g2d.getTransform();
        
        // Obter transformação interpolada
        AffineTransform interpolatedTransform = AnimationController.getInterpolatedTransform(keyframes, frame);
        g2d.transform(interpolatedTransform);
        
        // Obter cor e largura interpoladas
        Color interpolatedColor = AnimationController.getInterpolatedColor(keyframes, frame);
        float interpolatedStrokeWidth = AnimationController.getInterpolatedStrokeWidth(keyframes, frame);
        
        // Configurar gráficos
        g2d.setColor(interpolatedColor);
        g2d.setStroke(new BasicStroke(interpolatedStrokeWidth));
        
        // Chamar método de desenho específico da forma
        drawShape(g2d, frame);
        
        // Restaurar transformação original
        g2d.setTransform(originalTransform);
    }
    
    // Método abstrato para desenhar a forma básica
    protected abstract void drawShape(Graphics2D g2d, int frame);
    
    // Métodos de transformação que criam keyframes
    public void setPosition(int frame, int x, int y) {
        Keyframe keyframe = getOrCreateKeyframe(frame);
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        keyframe.setTransform(transform);
    }
    
    public void translate(int frame, int dx, int dy) {
        Keyframe keyframe = getOrCreateKeyframe(frame);
        AffineTransform transform = keyframe.getTransform();
        transform.translate(dx, dy);
    }
    
    public void rotate(int frame, double angle) {
        Keyframe keyframe = getOrCreateKeyframe(frame);
        AffineTransform transform = keyframe.getTransform();
        
        // Obter centro da forma para rotação
        int centerX = getBaseWidth() / 2;
        int centerY = getBaseHeight() / 2;
        
        // Aplicar rotação ao redor do centro
        transform.rotate(Math.toRadians(angle), centerX, centerY);
    }
    
    public void scale(int frame, double scaleX, double scaleY) {
        Keyframe keyframe = getOrCreateKeyframe(frame);
        AffineTransform transform = keyframe.getTransform();
        transform.scale(scaleX, scaleY);
    }
    
    // Atualizar propriedades no keyframe atual
    public void setColor(int frame, Color color) {
        this.baseColor = color;
        Keyframe keyframe = getOrCreateKeyframe(frame);
        keyframe.setColor(color);
    }
    
    public void setStrokeWidth(int frame, float strokeWidth) {
        this.baseStrokeWidth = strokeWidth;
        Keyframe keyframe = getOrCreateKeyframe(frame);
        keyframe.setStrokeWidth(strokeWidth);
    }
    
    // Métodos para obter propriedades interpoladas
    public Color getColor(int frame) {
        return AnimationController.getInterpolatedColor(keyframes, frame);
    }
    
    public float getStrokeWidth(int frame) {
        return AnimationController.getInterpolatedStrokeWidth(keyframes, frame);
    }
    
    public int getX(int frame) {
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        return (int) matrix[4]; // Componente de translação X
    }
    
    public int getY(int frame) {
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        return (int) matrix[5]; // Componente de translação Y
    }
    
    public int getWidth(int frame) {
        return (int) (getBaseWidth() * getScaleX(frame));
    }
    
    public int getHeight(int frame) {
        return (int) (getBaseHeight() * getScaleY(frame));
    }
    
    public double getScaleX(int frame) {
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        return Math.sqrt(matrix[0] * matrix[0] + matrix[1] * matrix[1]);
    }
    
    public double getScaleY(int frame) {
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        return Math.sqrt(matrix[2] * matrix[2] + matrix[3] * matrix[3]);
    }
    
    public double getRotation(int frame) {
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        return Math.atan2(matrix[1], matrix[0]);
    }
    
    // Obter ou criar keyframe para um frame específico
    protected Keyframe getOrCreateKeyframe(int frame) {
        for (Keyframe kf : keyframes) {
            if (kf.getFrame() == frame) {
                return kf;
            }
        }
        
        // Criar novo keyframe interpolado
        Keyframe newKeyframe = new Keyframe(frame);
        newKeyframe.setColor(baseColor);
        newKeyframe.setStrokeWidth(baseStrokeWidth);
        
        // Interpolar transformação do frame anterior
        if (!keyframes.isEmpty()) {
            AffineTransform interpolatedTransform = AnimationController.getInterpolatedTransform(keyframes, frame);
            newKeyframe.setTransform(new AffineTransform(interpolatedTransform));
        } else {
            // Se não há keyframes, criar transformação base
            updateKeyframeTransform(newKeyframe, frame);
        }
        
        keyframes.add(newKeyframe);
        sortKeyframes();
        return newKeyframe;
    }
    
    // Método para verificar se há keyframe em um frame específico
    public boolean hasKeyframeAt(int frame) {
        for (Keyframe kf : keyframes) {
            if (kf.getFrame() == frame) {
                return true;
            }
        }
        return false;
    }
    
    // Remover keyframe de um frame específico
    public void removeKeyframe(int frame) {
        keyframes.removeIf(kf -> kf.getFrame() == frame);
    }
    
    // Métodos de seleção
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public String getType() {
        return type;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    // Desenhar bounding box da seleção
    public void drawSelection(Graphics2D g2d, int frame) {
        if (!selected || !visible) return;
        
        int x = getX(frame);
        int y = getY(frame);
        int width = getWidth(frame);
        int height = getHeight(frame);
        
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                      10, new float[]{5, 5}, 0));
        g2d.drawRect(x - 5, y - 5, width + 10, height + 10);
        
        // Alças de redimensionamento
        g2d.setColor(Color.RED);
        g2d.fillRect(x - 4, y - 4, 8, 8);
        g2d.fillRect(x + width/2 - 4, y - 4, 8, 8);
        g2d.fillRect(x + width - 4, y - 4, 8, 8);
        g2d.fillRect(x + width - 4, y + height/2 - 4, 8, 8);
        g2d.fillRect(x + width - 4, y + height - 4, 8, 8);
        g2d.fillRect(x + width/2 - 4, y + height - 4, 8, 8);
        g2d.fillRect(x - 4, y + height - 4, 8, 8);
        g2d.fillRect(x - 4, y + height/2 - 4, 8, 8);
    }
}

// Classe para keyframes
class Keyframe implements Serializable {
    private int frame;
    private AffineTransform transform;
    private Color color;
    private float strokeWidth;
    
    public Keyframe(int frame) {
        this.frame = frame;
        this.transform = new AffineTransform();
        this.color = Color.BLACK;
        this.strokeWidth = 2.0f;
    }
    
    public int getFrame() { return frame; }
    public void setFrame(int frame) { this.frame = frame; }
    
    public AffineTransform getTransform() { return transform; }
    public void setTransform(AffineTransform transform) { this.transform = transform; }
    
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    
    public float getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(float strokeWidth) { this.strokeWidth = strokeWidth; }
}

// Classe para desenho livre com lápis
class PencilShape extends ShapeObject {
    private ArrayList<Point> points;
    
    public PencilShape(Color color, float strokeWidth) {
        super(color, strokeWidth, "Lápis");
        this.points = new ArrayList<>();
    }
    
    public void addPoint(Point point) {
        points.add(new Point(point));
    }
    
    public int getPointCount() {
        return points.size();
    }
    
    @Override
    protected void drawShape(Graphics2D g2d, int frame) {
        if (points.size() < 2) return;
        
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }
    
    @Override
    public boolean contains(Point point, int frame) {
        for (Point p : points) {
            if (p.distance(point) < baseStrokeWidth + 5) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void updateShape(Point point) {
        addPoint(point);
    }
    
    @Override
    public int getBaseX() {
        if (points.isEmpty()) return 0;
        int minX = points.get(0).x;
        for (Point p : points) {
            if (p.x < minX) minX = p.x;
        }
        return minX;
    }
    
    @Override
    public int getBaseY() {
        if (points.isEmpty()) return 0;
        int minY = points.get(0).y;
        for (Point p : points) {
            if (p.y < minY) minY = p.y;
        }
        return minY;
    }
    
    @Override
    public int getBaseWidth() {
        if (points.size() < 2) return 0;
        int minX = getBaseX();
        int maxX = minX;
        for (Point p : points) {
            if (p.x > maxX) maxX = p.x;
        }
        return maxX - minX;
    }
    
    @Override
    public int getBaseHeight() {
        if (points.size() < 2) return 0;
        int minY = getBaseY();
        int maxY = minY;
        for (Point p : points) {
            if (p.y > maxY) maxY = p.y;
        }
        return maxY - minY;
    }
    
    @Override
    protected void updateKeyframeTransform(Keyframe keyframe, int frame) {
        // Para PencilShape, a transformação é apenas translação
        AffineTransform transform = new AffineTransform();
        transform.translate(getBaseX(), getBaseY());
        keyframe.setTransform(transform);
    }
}

// Classe para quadrados/retângulos
class SquareShape extends ShapeObject {
    private int baseX, baseY, baseWidth, baseHeight;
    
    public SquareShape(int x, int y, int width, int height, Color color, float strokeWidth) {
        super(color, strokeWidth, "Quadrado");
        this.baseX = x;
        this.baseY = y;
        this.baseWidth = width;
        this.baseHeight = height;
    }
    
    @Override
    protected void drawShape(Graphics2D g2d, int frame) {
        g2d.fillRect(0, 0, baseWidth, baseHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, baseWidth, baseHeight);
    }
    
    @Override
    public boolean contains(Point point, int frame) {
        // Transformar ponto para coordenadas locais da forma
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        Point2D localPoint;
        try {
            AffineTransform inverse = transform.createInverse();
            localPoint = inverse.transform(point, null);
        } catch (Exception e) {
            return false;
        }
        
        return localPoint.getX() >= 0 && localPoint.getX() <= baseWidth &&
               localPoint.getY() >= 0 && localPoint.getY() <= baseHeight;
    }
    
    @Override
    public void updateShape(Point point) {
        baseWidth = Math.abs(point.x - baseX);
        baseHeight = Math.abs(point.y - baseY);
    }
    
    @Override
    public int getBaseX() { return baseX; }
    
    @Override
    public int getBaseY() { return baseY; }
    
    @Override
    public int getBaseWidth() { return baseWidth; }
    
    @Override
    public int getBaseHeight() { return baseHeight; }
    
    // Métodos específicos para SquareShape
    public void setBasePosition(int x, int y) {
        this.baseX = x;
        this.baseY = y;
    }
    
    public void setBaseSize(int width, int height) {
        this.baseWidth = width;
        this.baseHeight = height;
    }
}

// Classe para círculos
class CircleShape extends ShapeObject {
    private int centerX, centerY, radius;
    
    public CircleShape(int x, int y, int radius, Color color, float strokeWidth) {
        super(color, strokeWidth, "Círculo");
        this.centerX = x;
        this.centerY = y;
        this.radius = radius;
    }
    
    @Override
    protected void drawShape(Graphics2D g2d, int frame) {
        g2d.fillOval(-radius, -radius, radius * 2, radius * 2);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(-radius, -radius, radius * 2, radius * 2);
    }
    
    @Override
    public boolean contains(Point point, int frame) {
        // Transformar ponto para coordenadas locais da forma
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        Point2D localPoint;
        try {
            AffineTransform inverse = transform.createInverse();
            localPoint = inverse.transform(point, null);
        } catch (Exception e) {
            return false;
        }
        
        // Verificar se está dentro do círculo
        double dx = localPoint.getX();
        double dy = localPoint.getY();
        return dx * dx + dy * dy <= radius * radius;
    }
    
    @Override
    public void updateShape(Point point) {
        radius = (int) Math.sqrt(Math.pow(point.x - centerX, 2) + Math.pow(point.y - centerY, 2));
    }
    
    @Override
    public int getBaseX() { return centerX - radius; }
    
    @Override
    public int getBaseY() { return centerY - radius; }
    
    @Override
    public int getBaseWidth() { return radius * 2; }
    
    @Override
    public int getBaseHeight() { return radius * 2; }
    
    public int getRadius() { return radius; }
    
    public void setRadius(int radius) {
        this.radius = radius;
    }
}

// Classe para linhas
class LineShape extends ShapeObject {
    private int x1, y1, x2, y2;
    
    public LineShape(int x1, int y1, int x2, int y2, Color color, float strokeWidth) {
        super(color, strokeWidth, "Linha");
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    @Override
    protected void drawShape(Graphics2D g2d, int frame) {
        g2d.setStroke(new BasicStroke(baseStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
    }
    
    @Override
    public boolean contains(Point point, int frame) {
        // Transformar ponto para coordenadas locais da forma
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        Point2D localPoint;
        try {
            AffineTransform inverse = transform.createInverse();
            localPoint = inverse.transform(point, null);
        } catch (Exception e) {
            return false;
        }
        
        double distance = pointToLineDistance(
            (int) localPoint.getX(), (int) localPoint.getY(), 
            x1, y1, x2, y2
        );
        return distance < baseStrokeWidth + 5;
    }
    
    private double pointToLineDistance(int px, int py, int x1, int y1, int x2, int y2) {
        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;
        
        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = (lenSq != 0) ? dot / lenSq : -1;
        
        double xx, yy;
        
        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        double dx = px - xx;
        double dy = py - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    @Override
    public void updateShape(Point point) {
        x2 = point.x;
        y2 = point.y;
    }
    
    @Override
    public int getBaseX() { return Math.min(x1, x2); }
    
    @Override
    public int getBaseY() { return Math.min(y1, y2); }
    
    @Override
    public int getBaseWidth() { return Math.abs(x2 - x1); }
    
    @Override
    public int getBaseHeight() { return Math.abs(y2 - y1); }
    
    @Override
    protected void updateKeyframeTransform(Keyframe keyframe, int frame) {
        AffineTransform transform = new AffineTransform();
        transform.translate(getBaseX(), getBaseY());
        keyframe.setTransform(transform);
    }
}

// Classe para triângulos
class TriangleShape extends ShapeObject {
    private int centerX, centerY, size;
    
    public TriangleShape(int x, int y, int size, Color color, float strokeWidth) {
        super(color, strokeWidth, "Triângulo");
        this.centerX = x;
        this.centerY = y;
        this.size = size;
    }
    
    @Override
    protected void drawShape(Graphics2D g2d, int frame) {
        int[] xPoints = {0, -size/2, size/2};
        int[] yPoints = {-size/2, size/2, size/2};
        
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(xPoints, yPoints, 3);
    }
    
    @Override
    public boolean contains(Point point, int frame) {
        // Transformar ponto para coordenadas locais da forma
        AffineTransform transform = AnimationController.getInterpolatedTransform(keyframes, frame);
        Point2D localPoint;
        try {
            AffineTransform inverse = transform.createInverse();
            localPoint = inverse.transform(point, null);
        } catch (Exception e) {
            return false;
        }
        
        int[] xPoints = {0, -size/2, size/2};
        int[] yPoints = {-size/2, size/2, size/2};
        
        Polygon triangle = new Polygon(xPoints, yPoints, 3);
        return triangle.contains(localPoint);
    }
    
    @Override
    public void updateShape(Point point) {
        size = (int) Math.sqrt(Math.pow(point.x - centerX, 2) + Math.pow(point.y - centerY, 2)) * 2;
    }
    
    @Override
    public int getBaseX() { return centerX - size/2; }
    
    @Override
    public int getBaseY() { return centerY - size/2; }
    
    @Override
    public int getBaseWidth() { return size; }
    
    @Override
    public int getBaseHeight() { return size; }
    
    public int getSize() { return size; }
    
    public void setSize(int size) {
        this.size = size;
    }
}