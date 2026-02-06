import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

// Painel da timeline
class TimelinePanel extends JPanel {
    private AnimationEditor editor;
    private JSlider frameSlider;
    private JLabel frameLabel;
    private JButton playBtn, stopBtn, addKeyframeBtn, prevFrameBtn, nextFrameBtn;
    private JSpinner fpsSpinner;
    private JSpinner totalFramesSpinner;
    private javax.swing.Timer animationTimer;
    private boolean isPlaying = false;
    
    public TimelinePanel(AnimationEditor editor) {
        this.editor = editor;
        
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(1000, 180));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Timeline"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        initComponents();
        setupLayout();
        setupAnimationTimer();
    }
    
    private void initComponents() {
        // Botões de controle
        prevFrameBtn = new JButton("⏮");
        nextFrameBtn = new JButton("⏭");
        playBtn = new JButton("▶ Reproduzir");
        stopBtn = new JButton("⏹ Parar");
        addKeyframeBtn = new JButton("✚ Keyframe");
        
        prevFrameBtn.addActionListener(e -> moveToPreviousFrame());
        nextFrameBtn.addActionListener(e -> moveToNextFrame());
        playBtn.addActionListener(e -> playAnimation());
        stopBtn.addActionListener(e -> stopAnimation());
        addKeyframeBtn.addActionListener(e -> addKeyframe());
        
        // Configuração FPS
        fpsSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 60, 1));
        fpsSpinner.setPreferredSize(new Dimension(60, 25));
        fpsSpinner.addChangeListener(e -> {
            if (animationTimer != null) {
                int fps = (int) fpsSpinner.getValue();
                animationTimer.setDelay(1000 / fps);
            }
        });
        
        // Configuração total de frames
        totalFramesSpinner = new JSpinner(new SpinnerNumberModel(
            editor.getTotalFrames(),
            1,
            1000,
            1));
        totalFramesSpinner.setPreferredSize(new Dimension(80, 25));
        totalFramesSpinner.addChangeListener(e -> {
            int frames = (int) totalFramesSpinner.getValue();
            editor.setTotalFrames(frames);
            frameSlider.setMaximum(frames - 1);
        });
        
        // Slider de frames
        frameLabel = new JLabel("Frame: 0/" + editor.getTotalFrames(), SwingConstants.CENTER);
        
        frameSlider = new JSlider(0, editor.getTotalFrames() - 1, 0);
        frameSlider.setMajorTickSpacing(10);
        frameSlider.setMinorTickSpacing(1);
        frameSlider.setPaintTicks(true);
        frameSlider.setPaintLabels(true);
        
        frameSlider.addChangeListener(e -> {
            if (!frameSlider.getValueIsAdjusting()) {
                int frame = frameSlider.getValue();
                editor.setCurrentFrame(frame);
                frameLabel.setText("Frame: " + frame + "/" + editor.getTotalFrames());
                editor.getDrawingPanel().repaint();
                editor.getPropertiesPanel().updateProperties(editor.getSelectedShape(), frame);
            }
        });
    }
    
    private void setupLayout() {
        // Painel superior (controles)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        controlPanel.add(prevFrameBtn);
        controlPanel.add(nextFrameBtn);
        controlPanel.add(playBtn);
        controlPanel.add(stopBtn);
        controlPanel.add(addKeyframeBtn);
        controlPanel.add(new JSeparator(SwingConstants.VERTICAL));
        controlPanel.add(new JLabel("FPS:"));
        controlPanel.add(fpsSpinner);
        controlPanel.add(new JLabel("Total Frames:"));
        controlPanel.add(totalFramesSpinner);
        
        // Painel do slider
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(frameLabel, BorderLayout.NORTH);
        sliderPanel.add(frameSlider, BorderLayout.CENTER);
        
        add(controlPanel, BorderLayout.NORTH);
        add(sliderPanel, BorderLayout.CENTER);
        
        // Painel de visualização de keyframes
        JPanel keyframePanel = new KeyframeVisualizationPanel(editor);
        add(keyframePanel, BorderLayout.SOUTH);
    }
    
    private void setupAnimationTimer() {
        animationTimer = new javax.swing.Timer(1000 / 30, e -> {
            int nextFrame = editor.getCurrentFrame() + 1;
            if (nextFrame >= editor.getTotalFrames()) {
                nextFrame = 0; // Loop
            }
            updateFrame(nextFrame);
        });
    }
    
    public void addKeyframe() {
        if (editor.getSelectedShape() != null) {
            editor.getSelectedShape().addKeyframe(editor.getCurrentFrame());
            JOptionPane.showMessageDialog(this, 
                "Keyframe adicionado no frame " + editor.getCurrentFrame(),
                "Keyframe",
                JOptionPane.INFORMATION_MESSAGE);
            repaint();
        } else {
            JOptionPane.showMessageDialog(this,
                "Selecione uma forma primeiro!",
                "Aviso",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    public void updateFrame(int frame) {
        frameSlider.setValue(frame);
        frameLabel.setText("Frame: " + frame + "/" + editor.getTotalFrames());
        editor.setCurrentFrame(frame);
        editor.getDrawingPanel().repaint();
        editor.getPropertiesPanel().updateProperties(editor.getSelectedShape(), frame);
    }
    
    public void setPlaying(boolean playing) {
        isPlaying = playing;
        playBtn.setEnabled(!playing);
        stopBtn.setEnabled(playing);
        
        if (playing) {
            animationTimer.start();
        } else {
            animationTimer.stop();
        }
    }
    
    public void setTotalFrames(int frames) {
        totalFramesSpinner.setValue(frames);
        frameSlider.setMaximum(frames - 1);
        frameLabel.setText("Frame: " + editor.getCurrentFrame() + "/" + frames);
    }
    
    private void moveToPreviousFrame() {
        int currentFrame = editor.getCurrentFrame();
        if (currentFrame > 0) {
            updateFrame(currentFrame - 1);
        }
    }
    
    private void moveToNextFrame() {
        int currentFrame = editor.getCurrentFrame();
        if (currentFrame < editor.getTotalFrames() - 1) {
            updateFrame(currentFrame + 1);
        }
    }
    
    // Métodos públicos para controle da animação
    public void playAnimation() {
        setPlaying(true);
    }
    
    public void stopAnimation() {
        setPlaying(false);
    }
    
    // Painel para visualização de keyframes
    class KeyframeVisualizationPanel extends JPanel {
        private AnimationEditor editor;
        
        public KeyframeVisualizationPanel(AnimationEditor editor) {
            this.editor = editor;
            setPreferredSize(new Dimension(1000, 40));
            setBackground(Color.LIGHT_GRAY);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            int width = getWidth();
            int height = getHeight();
            int totalFrames = editor.getTotalFrames();
            
            // Desenhar linha do tempo
            g2d.setColor(Color.BLACK);
            g2d.drawLine(10, height/2, width - 10, height/2);
            
            // Marcar frames
            for (int i = 0; i < totalFrames; i++) {
                int x = 10 + (i * (width - 20) / totalFrames);
                
                // Marcador principal a cada 10 frames
                if (i % 10 == 0) {
                    g2d.setColor(Color.BLACK);
                    g2d.drawLine(x, height/2 - 5, x, height/2 + 5);
                    g2d.drawString(String.valueOf(i), x - 5, height - 5);
                } else {
                    g2d.setColor(Color.GRAY);
                    g2d.drawLine(x, height/2 - 3, x, height/2 + 3);
                }
            }
            
            // Desenhar keyframes das formas
            int shapeIndex = 0;
            for (ShapeObject shape : editor.getShapes()) {
                Color shapeColor = shape.getColor(editor.getCurrentFrame());
                
                for (Keyframe keyframe : shape.getKeyframes()) {
                    int frame = keyframe.getFrame();
                    int x = 10 + (frame * (width - 20) / totalFrames);
                    
                    // Desenhar marcador de keyframe
                    g2d.setColor(shapeColor);
                    int y = height/2 - 15 - (shapeIndex * 10);
                    g2d.fillRect(x - 3, y, 6, 10);
                    
                    // Desenhar linha para conectar keyframes
                    if (shape.getKeyframes().size() > 1) {
                        g2d.setColor(shapeColor.darker());
                        ArrayList<Keyframe> keyframesList = shape.getKeyframes();
                        keyframesList.sort((k1, k2) -> Integer.compare(k1.getFrame(), k2.getFrame()));
                        
                        for (int i = 0; i < keyframesList.size() - 1; i++) {
                            int x1 = 10 + (keyframesList.get(i).getFrame() * (width - 20) / totalFrames);
                            int x2 = 10 + (keyframesList.get(i + 1).getFrame() * (width - 20) / totalFrames);
                            g2d.drawLine(x1, y + 5, x2, y + 5);
                        }
                    }
                }
                shapeIndex++;
            }
            
            // Marcar frame atual
            int currentFrame = editor.getCurrentFrame();
            int currentX = 10 + (currentFrame * (width - 20) / totalFrames);
            g2d.setColor(Color.RED);
            g2d.drawLine(currentX, 0, currentX, height);
            
            // Desenhar triângulo indicador
            int[] xPoints = {currentX - 5, currentX + 5, currentX};
            int[] yPoints = {0, 0, 10};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
    }
}