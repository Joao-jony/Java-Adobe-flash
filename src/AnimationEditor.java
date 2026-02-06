import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

public class AnimationEditor extends JFrame {
    // Componentes
    private DrawingPanel drawingPanel;
    private TimelinePanel timelinePanel;
    private ToolPanel toolPanel;
    private PropertiesPanel propertiesPanel;
    
    // Estado
    private ArrayList<ShapeObject> shapes;
    private ShapeObject selectedShape;
    private PencilShape currentPencilStroke;
    private Tool currentTool;
    private Color currentColor;
    private int currentFrame;
    private int totalFrames;
    private float strokeWidth;
    
    // Constantes
    private static final int CANVAS_WIDTH = 1000;
    private static final int CANVAS_HEIGHT = 700;
    private static final int TIMELINE_HEIGHT = 180;
    private static final int TOOL_PANEL_WIDTH = 120;
    private static final int PROPERTIES_WIDTH = 250;
    private static final int DRAWING_PANEL_WIDTH = 2000;
    private static final int DRAWING_PANEL_HEIGHT = 2000;
    private static final int DEFAULT_TOTAL_FRAMES = 120;
    private static final int DEFAULT_FPS = 30;
    private static final float DEFAULT_STROKE_WIDTH = 2.0f;
    
    // Enumeração de ferramentas
    public enum Tool {
        SELECT("Seleção", 'V'),
        PENCIL("Lápis", 'P'),
        SQUARE("Quadrado", 'R'),
        CIRCLE("Círculo", 'C'),
        LINE("Linha", 'L'),
        TRIANGLE("Triângulo", 'T');
        
        private final String displayName;
        private final char shortcut;
        
        Tool(String displayName, char shortcut) {
            this.displayName = displayName;
            this.shortcut = shortcut;
        }
        
        public String getDisplayName() { return displayName; }
        public char getShortcut() { return shortcut; }
    }
    
    public AnimationEditor() {
        shapes = new ArrayList<>();
        currentTool = Tool.SELECT;
        currentColor = Color.BLACK;
        strokeWidth = DEFAULT_STROKE_WIDTH;
        currentFrame = 0;
        totalFrames = DEFAULT_TOTAL_FRAMES;
        
        initUI();
        setupMenu();
        setupKeyboardShortcuts();
    }
    
    private void initUI() {
        setTitle("Adobe Flash - mini");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Painel de ferramentas
        toolPanel = new ToolPanel(this);
        add(toolPanel, BorderLayout.WEST);
        
        // Painel de desenho
        drawingPanel = new DrawingPanel(this);
        JScrollPane scrollPane = new JScrollPane(drawingPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
        
        // Timeline
        timelinePanel = new TimelinePanel(this);
        add(timelinePanel, BorderLayout.SOUTH);
        
        // Painel de propriedades
        propertiesPanel = new PropertiesPanel(this);
        add(propertiesPanel, BorderLayout.EAST);
        
        setSize(1400, 900);
        setLocationRelativeTo(null);
    }
    
    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        
        // Menu Arquivo
        JMenu fileMenu = new JMenu("Arquivo");
        JMenuItem newItem = new JMenuItem("Novo");
        JMenuItem openItem = new JMenuItem("Abrir");
        JMenuItem saveItem = new JMenuItem("Salvar");
        JMenuItem exitItem = new JMenuItem("Sair");
        
        newItem.addActionListener(e -> newAnimation());
        openItem.addActionListener(e -> openAnimation());
        saveItem.addActionListener(e -> saveAnimation());
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Menu Editar
        JMenu editMenu = new JMenu("Editar");
        JMenuItem deleteItem = new JMenuItem("Deletar");
        deleteItem.addActionListener(e -> deleteSelectedShape());
        editMenu.add(deleteItem);
        
        // Menu Animação
        JMenu animMenu = new JMenu("Animação");
        JMenuItem playItem = new JMenuItem("Reproduzir");
        JMenuItem stopItem = new JMenuItem("Parar");
        JMenuItem addKeyframeItem = new JMenuItem("Adicionar Keyframe");
        
        playItem.addActionListener(e -> timelinePanel.playAnimation());
        stopItem.addActionListener(e -> timelinePanel.stopAnimation());
        addKeyframeItem.addActionListener(e -> timelinePanel.addKeyframe());
        
        animMenu.add(playItem);
        animMenu.add(stopItem);
        animMenu.addSeparator();
        animMenu.add(addKeyframeItem);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(animMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void setupKeyboardShortcuts() {
        InputMap inputMap = drawingPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = drawingPanel.getActionMap();
        
        // Ferramentas
        for (Tool tool : Tool.values()) {
            inputMap.put(KeyStroke.getKeyStroke(Character.toString(tool.getShortcut())), tool.name());
            actionMap.put(tool.name(), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setCurrentTool(tool);
                    toolPanel.updateToolSelection(tool);
                }
            });
        }
        
        // Outros atalhos
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), "DELETE");
        actionMap.put("DELETE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedShape();
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        actionMap.put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedShape = null;
                drawingPanel.repaint();
            }
        });
    }
    
    // Getters e Setters
    public ArrayList<ShapeObject> getShapes() { return shapes; }
    public ShapeObject getSelectedShape() { return selectedShape; }
    public void setSelectedShape(ShapeObject shape) { 
        this.selectedShape = shape; 
        if (shape != null) {
            propertiesPanel.updateProperties(shape, currentFrame);
        }
    }
    
    public PencilShape getCurrentPencilStroke() { return currentPencilStroke; }
    public void setCurrentPencilStroke(PencilShape stroke) { this.currentPencilStroke = stroke; }
    
    public Tool getCurrentTool() { return currentTool; }
    public void setCurrentTool(Tool tool) { this.currentTool = tool; }
    
    public Color getCurrentColor() { return currentColor; }
    public void setCurrentColor(Color color) { this.currentColor = color; }
    
    public int getCurrentFrame() { return currentFrame; }
    public void setCurrentFrame(int frame) { 
        this.currentFrame = frame; 
        // Atualizar propriedades quando o frame mudar
        if (selectedShape != null) {
            propertiesPanel.updateProperties(selectedShape, frame);
        }
    }
    
    public int getTotalFrames() { return totalFrames; }
    public void setTotalFrames(int frames) { 
        this.totalFrames = frames; 
        timelinePanel.setTotalFrames(frames);
    }
    
    public float getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(float width) { this.strokeWidth = width; }
    
    public DrawingPanel getDrawingPanel() { return drawingPanel; }
    public PropertiesPanel getPropertiesPanel() { return propertiesPanel; }
    public TimelinePanel getTimelinePanel() { return timelinePanel; }
    
    // Métodos de animação (removidos os duplicados - agora estão no TimelinePanel)
    // As chamadas para playAnimation() e stopAnimation() agora vão para timelinePanel
    
    public void newAnimation() {
        int result = JOptionPane.showConfirmDialog(this, 
            "Deseja salvar a animação atual antes de criar uma nova?", 
            "Nova Animação", 
            JOptionPane.YES_NO_CANCEL_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            saveAnimation();
        }
        
        if (result != JOptionPane.CANCEL_OPTION) {
            shapes.clear();
            currentFrame = 0;
            selectedShape = null;
            timelinePanel.updateFrame(0);
            drawingPanel.repaint();
            propertiesPanel.updateProperties(null, 0);
        }
    }
    
    public void saveAnimation() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Animação");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Arquivos de Animação (*.anim)", "anim"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".anim")) {
                file = new File(file.getAbsolutePath() + ".anim");
            }
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                AnimationData data = new AnimationData(shapes, totalFrames, currentFrame);
                oos.writeObject(data);
                JOptionPane.showMessageDialog(this, "Animação salva com sucesso!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao salvar animação: " + ex.getMessage());
            }
        }
    }
    
    public void openAnimation() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Abrir Animação");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Arquivos de Animação (*.anim)", "anim"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                AnimationData data = (AnimationData) ois.readObject();
                shapes = data.getShapes();
                totalFrames = data.getTotalFrames();
                currentFrame = data.getCurrentFrame();
                
                timelinePanel.setTotalFrames(totalFrames);
                timelinePanel.updateFrame(currentFrame);
                drawingPanel.repaint();
                propertiesPanel.updateProperties(null, currentFrame);
                JOptionPane.showMessageDialog(this, "Animação carregada com sucesso!");
            } catch (IOException | ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao abrir animação: " + ex.getMessage());
            }
        }
    }
    
    private void deleteSelectedShape() {
        if (selectedShape != null) {
            shapes.remove(selectedShape);
            selectedShape = null;
            drawingPanel.repaint();
            propertiesPanel.updateProperties(null, currentFrame);
        }
    }
    
    // Classe interna para dados de animação
    class AnimationData implements Serializable {
        private ArrayList<ShapeObject> shapes;
        private int totalFrames;
        private int currentFrame;
        
        public AnimationData(ArrayList<ShapeObject> shapes, int totalFrames, int currentFrame) {
            this.shapes = new ArrayList<>(shapes);
            this.totalFrames = totalFrames;
            this.currentFrame = currentFrame;
        }
        
        public ArrayList<ShapeObject> getShapes() { return shapes; }
        public int getTotalFrames() { return totalFrames; }
        public int getCurrentFrame() { return currentFrame; }
    }
    
    // Método main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            AnimationEditor editor = new AnimationEditor();
            editor.setVisible(true);
        });
    }
}