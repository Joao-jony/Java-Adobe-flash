import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// Painel de desenho principal
class DrawingPanel extends JPanel {
    private AnimationEditor editor;
    private static final int GRID_SIZE = 20;
    private static final Color GRID_COLOR = new Color(240, 240, 240);
    private static final Color CENTER_LINE_COLOR = new Color(200, 200, 200);
    
    public DrawingPanel(AnimationEditor editor) {
        this.editor = editor;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(2000, 2000));
        
        DrawingMouseHandler handler = new DrawingMouseHandler(editor);
        addMouseListener(handler);
        addMouseMotionListener(handler);
        addMouseWheelListener(handler);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Configurar qualidade de renderização
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        // Desenhar grade de referência
        drawGrid(g2d);
        
        int currentFrame = editor.getCurrentFrame();
        
        // Desenhar todas as formas usando o frame atual
        for (ShapeObject shape : editor.getShapes()) {
            shape.draw(g2d, currentFrame);
        }
        
        // Desenhar traço atual do lápis
        if (editor.getCurrentPencilStroke() != null) {
            editor.getCurrentPencilStroke().draw(g2d, currentFrame);
        }
        
        // Desenhar bounding box da forma selecionada
        if (editor.getSelectedShape() != null) {
            editor.getSelectedShape().drawSelection(g2d, currentFrame);
        }
        
        // Desenhar informação do frame atual
        drawFrameInfo(g2d);
    }
    
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(GRID_COLOR);
        
        for (int x = 0; x < getWidth(); x += GRID_SIZE) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        
        for (int y = 0; y < getHeight(); y += GRID_SIZE) {
            g2d.drawLine(0, y, getWidth(), y);
        }
        
        // Linhas centrais
        g2d.setColor(CENTER_LINE_COLOR);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        g2d.drawLine(centerX, 0, centerX, getHeight());
        g2d.drawLine(0, centerY, getWidth(), centerY);
    }
    
    private void drawFrameInfo(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String info = "Frame: " + editor.getCurrentFrame() + "/" + editor.getTotalFrames();
        g2d.drawString(info, 10, 20);
    }
}

// Manipulador de eventos do mouse para desenho
class DrawingMouseHandler extends MouseAdapter {
    private AnimationEditor editor;
    private Point startPoint;
    private ShapeObject newShape;
    private boolean isDragging;
    private int dragHandle = -1;
    
    public DrawingMouseHandler(AnimationEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = e.getPoint();
        isDragging = false;
        dragHandle = -1;
        
        if (editor.getCurrentTool() == AnimationEditor.Tool.SELECT) {
            // Verificar se clicou em uma forma
            editor.setSelectedShape(null);
            for (int i = editor.getShapes().size() - 1; i >= 0; i--) {
                ShapeObject shape = editor.getShapes().get(i);
                if (shape.contains(startPoint, editor.getCurrentFrame())) {
                    editor.setSelectedShape(shape);
                    shape.setSelected(true);
                    editor.getPropertiesPanel().updateProperties(shape, editor.getCurrentFrame());
                    
                    // Verificar se clicou em uma alça de redimensionamento
                    dragHandle = getHandleAtPoint(shape, startPoint, editor.getCurrentFrame());
                    break;
                }
            }
            
            // Desselecionar outras formas
            for (ShapeObject shape : editor.getShapes()) {
                if (shape != editor.getSelectedShape()) {
                    shape.setSelected(false);
                }
            }
        } else if (editor.getCurrentTool() == AnimationEditor.Tool.PENCIL) {
            // Iniciar novo traço de lápis
            editor.setCurrentPencilStroke(new PencilShape(editor.getCurrentColor(), editor.getStrokeWidth()));
            editor.getCurrentPencilStroke().addPoint(startPoint);
        } else {
            // Criar nova forma geométrica
            switch (editor.getCurrentTool()) {
                case SQUARE:
                    newShape = new SquareShape(startPoint.x, startPoint.y, 0, 0, 
                                               editor.getCurrentColor(), editor.getStrokeWidth());
                    break;
                case CIRCLE:
                    newShape = new CircleShape(startPoint.x, startPoint.y, 0, 
                                               editor.getCurrentColor(), editor.getStrokeWidth());
                    break;
                case LINE:
                    newShape = new LineShape(startPoint.x, startPoint.y, startPoint.x, startPoint.y, 
                                             editor.getCurrentColor(), editor.getStrokeWidth());
                    break;
                case TRIANGLE:
                    newShape = new TriangleShape(startPoint.x, startPoint.y, 50, 
                                                 editor.getCurrentColor(), editor.getStrokeWidth());
                    break;
            }
            if (newShape != null) {
                // Adicionar keyframe inicial
                newShape.addKeyframe(editor.getCurrentFrame());
                editor.getShapes().add(newShape);
                editor.setSelectedShape(newShape);
                newShape.setSelected(true);
                editor.getPropertiesPanel().updateProperties(newShape, editor.getCurrentFrame());
            }
        }
        editor.getDrawingPanel().repaint();
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        Point currentPoint = e.getPoint();
        isDragging = true;
        
        if (editor.getCurrentTool() == AnimationEditor.Tool.SELECT && editor.getSelectedShape() != null) {
            if (dragHandle == -1) {
                // Mover forma selecionada
                int dx = currentPoint.x - startPoint.x;
                int dy = currentPoint.y - startPoint.y;
                
                // Criar keyframe para a nova posição
                editor.getSelectedShape().translate(editor.getCurrentFrame(), dx, dy);
                
                startPoint = currentPoint;
                editor.getPropertiesPanel().updateProperties(editor.getSelectedShape(), editor.getCurrentFrame());
            } else {
                // Redimensionar forma
                resizeShape(currentPoint);
            }
        } else if (editor.getCurrentTool() == AnimationEditor.Tool.PENCIL && editor.getCurrentPencilStroke() != null) {
            // Continuar traço de lápis
            editor.getCurrentPencilStroke().addPoint(currentPoint);
        } else if (newShape != null) {
            // Atualizar forma sendo criada
            newShape.updateShape(currentPoint);
            editor.getPropertiesPanel().updateProperties(newShape, editor.getCurrentFrame());
        }
        
        editor.getDrawingPanel().repaint();
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (editor.getCurrentTool() == AnimationEditor.Tool.PENCIL && editor.getCurrentPencilStroke() != null 
            && editor.getCurrentPencilStroke().getPointCount() > 1) {
            // Finalizar traço de lápis e adicionar à lista de formas
            editor.getCurrentPencilStroke().addKeyframe(editor.getCurrentFrame());
            editor.getShapes().add(editor.getCurrentPencilStroke());
            editor.setSelectedShape(editor.getCurrentPencilStroke());
            editor.getCurrentPencilStroke().setSelected(true);
            editor.getPropertiesPanel().updateProperties(editor.getSelectedShape(), editor.getCurrentFrame());
            editor.setCurrentPencilStroke(null);
        } else if (newShape != null && isDragging) {
            // Finalizar forma geométrica com keyframe
            newShape.addKeyframe(editor.getCurrentFrame());
            newShape = null;
        }
        
        editor.getDrawingPanel().repaint();
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (editor.getSelectedShape() != null && e.isControlDown()) {
            // Zoom com Ctrl + Scroll
            double scaleFactor = e.getWheelRotation() > 0 ? 0.9 : 1.1;
            editor.getSelectedShape().scale(editor.getCurrentFrame(), scaleFactor, scaleFactor);
            editor.getPropertiesPanel().updateProperties(editor.getSelectedShape(), editor.getCurrentFrame());
            editor.getDrawingPanel().repaint();
        }
    }
    
    private int getHandleAtPoint(ShapeObject shape, Point point, int frame) {
        int x = shape.getX(frame);
        int y = shape.getY(frame);
        int width = shape.getWidth(frame);
        int height = shape.getHeight(frame);
        
        // Definir regiões para cada alça
        Rectangle[] handles = {
            new Rectangle(x - 4, y - 4, 8, 8),      // 0: Canto superior esquerdo
            new Rectangle(x + width/2 - 4, y - 4, 8, 8), // 1: Superior meio
            new Rectangle(x + width - 4, y - 4, 8, 8),  // 2: Canto superior direito
            new Rectangle(x + width - 4, y + height/2 - 4, 8, 8), // 3: Direita meio
            new Rectangle(x + width - 4, y + height - 4, 8, 8), // 4: Canto inferior direito
            new Rectangle(x + width/2 - 4, y + height - 4, 8, 8), // 5: Inferior meio
            new Rectangle(x - 4, y + height - 4, 8, 8), // 6: Canto inferior esquerdo
            new Rectangle(x - 4, y + height/2 - 4, 8, 8)  // 7: Esquerda meio
        };
        
        for (int i = 0; i < handles.length; i++) {
            if (handles[i].contains(point)) {
                return i;
            }
        }
        
        return -1;
    }
    
    private void resizeShape(Point currentPoint) {
        ShapeObject shape = editor.getSelectedShape();
        int frame = editor.getCurrentFrame();
        
        int x = shape.getX(frame);
        int y = shape.getY(frame);
        int width = shape.getWidth(frame);
        int height = shape.getHeight(frame);
        
        int dx = currentPoint.x - startPoint.x;
        int dy = currentPoint.y - startPoint.y;
        
        switch (dragHandle) {
            case 0: // Canto superior esquerdo
                shape.setPosition(frame, x + dx, y + dy);
                shape.scale(frame, (double)(width - dx) / width, (double)(height - dy) / height);
                break;
            case 1: // Superior meio
                shape.setPosition(frame, x, y + dy);
                shape.scale(frame, 1.0, (double)(height - dy) / height);
                break;
            case 2: // Canto superior direito
                shape.setPosition(frame, x, y + dy);
                shape.scale(frame, (double)(width + dx) / width, (double)(height - dy) / height);
                break;
            case 3: // Direita meio
                shape.scale(frame, (double)(width + dx) / width, 1.0);
                break;
            case 4: // Canto inferior direito
                shape.scale(frame, (double)(width + dx) / width, (double)(height + dy) / height);
                break;
            case 5: // Inferior meio
                shape.scale(frame, 1.0, (double)(height + dy) / height);
                break;
            case 6: // Canto inferior esquerdo
                shape.setPosition(frame, x + dx, y);
                shape.scale(frame, (double)(width - dx) / width, (double)(height + dy) / height);
                break;
            case 7: // Esquerda meio
                shape.setPosition(frame, x + dx, y);
                shape.scale(frame, (double)(width - dx) / width, 1.0);
                break;
        }
        
        startPoint = currentPoint;
        editor.getPropertiesPanel().updateProperties(shape, frame);
    }
}

// Painel de ferramentas
class ToolPanel extends JPanel {
    private AnimationEditor editor;
    private JToggleButton selectBtn, pencilBtn;
    private JButton squareBtn, circleBtn, lineBtn, triangleBtn;
    private JSlider strokeSlider;
    private JLabel strokeLabel;
    
    public ToolPanel(AnimationEditor editor) {
        this.editor = editor;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(120, 700));
        setBorder(BorderFactory.createTitledBorder("Ferramentas"));
        
        initComponents();
        setupLayout();
    }
    
    private void initComponents() {
        // Grupo para botões de ferramentas
        ButtonGroup toolGroup = new ButtonGroup();
        
        // Criar botões com ícones
        selectBtn = createToolButton("Seleção", 'V', true);
        pencilBtn = createToolButton("Lápis", 'P', false);
        
        toolGroup.add(selectBtn);
        toolGroup.add(pencilBtn);
        
        // Botões de formas
        squareBtn = createShapeButton("Quadrado", 'R');
        circleBtn = createShapeButton("Círculo", 'C');
        lineBtn = createShapeButton("Linha", 'L');
        triangleBtn = createShapeButton("Triângulo", 'T');
        
        // Slider para espessura do traço
        strokeLabel = new JLabel("Espessura: " + editor.getStrokeWidth());
        strokeSlider = new JSlider(1, 50, (int) editor.getStrokeWidth());
        strokeSlider.addChangeListener(e -> {
            float newWidth = strokeSlider.getValue();
            editor.setStrokeWidth(newWidth);
            strokeLabel.setText("Espessura: " + newWidth);
            
            if (editor.getSelectedShape() != null) {
                editor.getSelectedShape().setStrokeWidth(editor.getCurrentFrame(), newWidth);
                editor.getDrawingPanel().repaint();
            }
        });
    }
    
    private void setupLayout() {
        add(Box.createVerticalStrut(10));
        add(selectBtn);
        add(Box.createVerticalStrut(5));
        add(pencilBtn);
        add(Box.createVerticalStrut(20));
        add(new JSeparator());
        add(Box.createVerticalStrut(10));
        add(squareBtn);
        add(Box.createVerticalStrut(5));
        add(circleBtn);
        add(Box.createVerticalStrut(5));
        add(lineBtn);
        add(Box.createVerticalStrut(5));
        add(triangleBtn);
        add(Box.createVerticalStrut(20));
        add(new JSeparator());
        add(Box.createVerticalStrut(10));
        add(strokeLabel);
        add(Box.createVerticalStrut(5));
        add(strokeSlider);
        add(Box.createVerticalGlue());
        
        // Painel de cores
        add(createColorPanel());
    }
    
    private JToggleButton createToolButton(String text, char shortcut, boolean selected) {
        JToggleButton button = new JToggleButton(
            "<html><center><b>" + shortcut + "</b><br>" + text + "</center></html>");
        button.setSelected(selected);
        button.setFocusable(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(100, 60));
        
        button.addActionListener(e -> {
            if (text.equals("Seleção")) {
                editor.setCurrentTool(AnimationEditor.Tool.SELECT);
            } else if (text.equals("Lápis")) {
                editor.setCurrentTool(AnimationEditor.Tool.PENCIL);
            }
        });
        
        return button;
    }
    
    private JButton createShapeButton(String text, char shortcut) {
        JButton button = new JButton(
            "<html><center><b>" + shortcut + "</b><br>" + text + "</center></html>");
        button.setFocusable(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(100, 60));
        
        button.addActionListener(e -> {
            switch (text) {
                case "Quadrado": editor.setCurrentTool(AnimationEditor.Tool.SQUARE); break;
                case "Círculo": editor.setCurrentTool(AnimationEditor.Tool.CIRCLE); break;
                case "Linha": editor.setCurrentTool(AnimationEditor.Tool.LINE); break;
                case "Triângulo": editor.setCurrentTool(AnimationEditor.Tool.TRIANGLE); break;
            }
            selectBtn.setSelected(false);
            pencilBtn.setSelected(false);
        });
        
        return button;
    }
    
    private JPanel createColorPanel() {
        JPanel colorPanel = new JPanel(new FlowLayout());
        colorPanel.setBorder(BorderFactory.createTitledBorder("Cores"));
        
        Color[] colors = {
            Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.ORANGE
        };
        
        for (Color color : colors) {
            JButton colorBtn = new JButton();
            colorBtn.setBackground(color);
            colorBtn.setPreferredSize(new Dimension(25, 25));
            colorBtn.addActionListener(e -> {
                editor.setCurrentColor(color);
                if (editor.getSelectedShape() != null) {
                    editor.getSelectedShape().setColor(editor.getCurrentFrame(), color);
                    editor.getDrawingPanel().repaint();
                }
            });
            colorPanel.add(colorBtn);
        }
        
        JButton customColorBtn = new JButton("Mais...");
        customColorBtn.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                editor, 
                "Escolher Cor", 
                editor.getCurrentColor()
            );
            if (newColor != null) {
                editor.setCurrentColor(newColor);
                if (editor.getSelectedShape() != null) {
                    editor.getSelectedShape().setColor(editor.getCurrentFrame(), newColor);
                    editor.getDrawingPanel().repaint();
                }
            }
        });
        
        colorPanel.add(customColorBtn);
        return colorPanel;
    }
    
    public void updateToolSelection(AnimationEditor.Tool tool) {
        selectBtn.setSelected(tool == AnimationEditor.Tool.SELECT);
        pencilBtn.setSelected(tool == AnimationEditor.Tool.PENCIL);
    }
}

// Painel de propriedades
class PropertiesPanel extends JPanel {
    private AnimationEditor editor;
    private JTextField xField, yField, widthField, heightField;
    private JSlider rotationSlider, scaleSlider;
    private JColorChooser colorChooser;
    private JLabel shapeTypeLabel;
    private JCheckBox keyframeCheckbox;
    
    public PropertiesPanel(AnimationEditor editor) {
        this.editor = editor;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(250, 700));
        setBorder(BorderFactory.createTitledBorder("Propriedades"));
        
        initComponents();
        setupLayout();
    }
    
    private void initComponents() {
        // Informações da forma
        shapeTypeLabel = new JLabel("Nenhuma forma selecionada");
        
        // Checkbox para keyframe
        keyframeCheckbox = new JCheckBox("Keyframe no frame atual");
        keyframeCheckbox.addActionListener(e -> {
            if (editor.getSelectedShape() != null) {
                if (keyframeCheckbox.isSelected()) {
                    editor.getSelectedShape().addKeyframe(editor.getCurrentFrame());
                } else {
                    editor.getSelectedShape().removeKeyframe(editor.getCurrentFrame());
                }
                editor.getDrawingPanel().repaint();
            }
        });
        
        // Campos de posição e tamanho
        xField = new JTextField("0", 8);
        yField = new JTextField("0", 8);
        widthField = new JTextField("0", 8);
        heightField = new JTextField("0", 8);
        
        // Sliders
        rotationSlider = new JSlider(-180, 180, 0);
        rotationSlider.addChangeListener(e -> {
            if (editor.getSelectedShape() != null && !rotationSlider.getValueIsAdjusting()) {
                double angle = rotationSlider.getValue();
                editor.getSelectedShape().rotate(editor.getCurrentFrame(), angle);
                editor.getDrawingPanel().repaint();
            }
        });
        
        scaleSlider = new JSlider(10, 500, 100);
        scaleSlider.addChangeListener(e -> {
            if (editor.getSelectedShape() != null && !scaleSlider.getValueIsAdjusting()) {
                double scale = scaleSlider.getValue() / 100.0;
                editor.getSelectedShape().scale(editor.getCurrentFrame(), scale, scale);
                editor.getDrawingPanel().repaint();
            }
        });
        
        // Color chooser
        colorChooser = new JColorChooser(editor.getCurrentColor());
        colorChooser.setPreviewPanel(new JPanel());
        colorChooser.getSelectionModel().addChangeListener(e -> {
            if (editor.getSelectedShape() != null) {
                editor.getSelectedShape().setColor(
                    editor.getCurrentFrame(), 
                    colorChooser.getColor()
                );
                editor.getDrawingPanel().repaint();
            }
        });
        
        // Configurar listeners para os campos de texto
        ActionListener fieldListener = e -> applyTransformations();
        xField.addActionListener(fieldListener);
        yField.addActionListener(fieldListener);
        widthField.addActionListener(fieldListener);
        heightField.addActionListener(fieldListener);
    }
    
    private void setupLayout() {
        // Painel de informações
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(shapeTypeLabel);
        add(infoPanel);
        
        add(keyframeCheckbox);
        add(Box.createVerticalStrut(10));
        
        // Painel de transformações
        JPanel transformPanel = new JPanel();
        transformPanel.setLayout(new BoxLayout(transformPanel, BoxLayout.Y_AXIS));
        transformPanel.setBorder(BorderFactory.createTitledBorder("Transformações"));
        
        // Posição
        JPanel positionPanel = createLabeledField("X:", xField);
        positionPanel.add(Box.createHorizontalStrut(10));
        positionPanel.add(createLabeledField("Y:", yField));
        
        // Tamanho
        JPanel sizePanel = createLabeledField("Largura:", widthField);
        sizePanel.add(Box.createHorizontalStrut(10));
        sizePanel.add(createLabeledField("Altura:", heightField));
        
        // Rotação
        JPanel rotationPanel = new JPanel(new BorderLayout());
        rotationPanel.setBorder(BorderFactory.createTitledBorder("Rotação (°)"));
        rotationPanel.add(rotationSlider, BorderLayout.CENTER);
        
        // Escala
        JPanel scalePanel = new JPanel(new BorderLayout());
        scalePanel.setBorder(BorderFactory.createTitledBorder("Escala (%)"));
        scalePanel.add(scaleSlider, BorderLayout.CENTER);
        
        // Organização do painel de transformações
        transformPanel.add(positionPanel);
        transformPanel.add(Box.createVerticalStrut(5));
        transformPanel.add(sizePanel);
        transformPanel.add(Box.createVerticalStrut(10));
        transformPanel.add(rotationPanel);
        transformPanel.add(Box.createVerticalStrut(5));
        transformPanel.add(scalePanel);
        
        add(transformPanel);
        add(Box.createVerticalStrut(10));
        
        // Painel de cores
        colorChooser.setBorder(BorderFactory.createTitledBorder("Cor da Forma"));
        add(colorChooser);
    }
    
    private JPanel createLabeledField(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }
    
    public void updateProperties(ShapeObject shape, int frame) {
        if (shape != null) {
            shapeTypeLabel.setText("Tipo: " + shape.getType());
            
            // Obter valores interpolados para o frame atual
            xField.setText(String.valueOf(shape.getX(frame)));
            yField.setText(String.valueOf(shape.getY(frame)));
            widthField.setText(String.valueOf(shape.getWidth(frame)));
            heightField.setText(String.valueOf(shape.getHeight(frame)));
            
            // Atualizar checkbox de keyframe
            keyframeCheckbox.setSelected(shape.hasKeyframeAt(frame));
            
            // Atualizar sliders
            double rotation = Math.toDegrees(shape.getRotation(frame));
            rotationSlider.setValue((int) rotation);
            
            double scale = shape.getScaleX(frame) * 100;
            scaleSlider.setValue((int) scale);
            
            colorChooser.setColor(shape.getColor(frame));
        } else {
            shapeTypeLabel.setText("Nenhuma forma selecionada");
            xField.setText("0");
            yField.setText("0");
            widthField.setText("0");
            heightField.setText("0");
            keyframeCheckbox.setSelected(false);
            rotationSlider.setValue(0);
            scaleSlider.setValue(100);
        }
    }
    
    private void applyTransformations() {
        if (editor.getSelectedShape() != null) {
            try {
                int x = Integer.parseInt(xField.getText());
                int y = Integer.parseInt(yField.getText());
                int width = Integer.parseInt(widthField.getText());
                int height = Integer.parseInt(heightField.getText());
                
                // Aplicar transformações
                editor.getSelectedShape().setPosition(editor.getCurrentFrame(), x, y);
                
                // Ajustar escala baseada no tamanho
                double scaleX = (double) width / editor.getSelectedShape().getBaseWidth();
                double scaleY = (double) height / editor.getSelectedShape().getBaseHeight();
                editor.getSelectedShape().scale(editor.getCurrentFrame(), scaleX, scaleY);
                
                editor.getDrawingPanel().repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Por favor, insira valores numéricos válidos!",
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}