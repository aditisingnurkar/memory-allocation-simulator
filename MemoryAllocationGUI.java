import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.awt.Color;

// ══════════════════════════════════════════════════════════════
//  DATA CLASSES
// ══════════════════════════════════════════════════════════════

class Memory {
    int id, size;
    boolean allocated;
    int processId, processSize;
    Memory(int id, int size) { this.id=id; this.size=size; allocated=false; processId=-1; processSize=0; }
}

class Process {
    int id, size;
    boolean allocated;
    String status;
    Process(int id, int size) { this.id=id; this.size=size; allocated=false; status="Not Allocated"; }
}

// ══════════════════════════════════════════════════════════════
//  CORE LOGIC  (untouched)
// ══════════════════════════════════════════════════════════════

class MemoryAllocator {
    static int firstInternal=0,firstExternal=0,bestInternal=0,bestExternal=0,worstInternal=0,worstExternal=0;

    static void reset(Memory[] m, Process[] p) {
        for (Memory x:m){x.allocated=false;x.processId=-1;x.processSize=0;}
        for (Process x:p){x.allocated=false;x.status="Not Allocated";}
    }

    static int externalFrag(Memory[] m, Process[] p) {
        int ext=0;
        for (Memory mem:m) {
            if (!mem.allocated) {
                boolean ok=false;
                for (Process proc:p) if (!proc.allocated&&proc.size<=mem.size){ok=true;break;}
                if (!ok) ext+=mem.size;
            }
        }
        return ext;
    }

    static void allocate(Memory[] m, Process[] p, String strategy) {
        reset(m,p);
        for (int i=0;i<p.length;i++) {
            int sel=-1;
            if (strategy.equals("FIRST")) {
                for (int j=0;j<m.length;j++) if (!m[j].allocated&&p[i].size<=m[j].size){sel=j;break;}
            } else if (strategy.equals("BEST")) {
                int minD=Integer.MAX_VALUE;
                for (int j=0;j<m.length;j++) if (!m[j].allocated&&p[i].size<=m[j].size){int d=m[j].size-p[i].size;if(d<minD){minD=d;sel=j;}}
            } else {
                int maxD=-1;
                for (int j=0;j<m.length;j++) if (!m[j].allocated&&p[i].size<=m[j].size){int d=m[j].size-p[i].size;if(d>maxD){maxD=d;sel=j;}}
            }
            if (sel!=-1){m[sel].allocated=true;m[sel].processId=p[i].id;m[sel].processSize=p[i].size;p[i].allocated=true;p[i].status="Allocated";}
            else p[i].status="Not Allocated";
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  COLOUR PALETTE
// ══════════════════════════════════════════════════════════════

class Pal {
    static final Color BG      = new Color(14,16,24);
    static final Color CARD    = new Color(20,24,36);
    static final Color BORDER  = new Color(38,46,68);
    static final Color TEXT    = new Color(215,220,235);
    static final Color MUTED   = new Color(110,120,148);
    static final Color ACCENT  = new Color(94,172,255);
    static final Color SUCCESS = new Color(52,211,153);
    static final Color WARN    = new Color(251,191,36);
    static final Color DANGER  = new Color(248,113,113);
    static final Color FRAG    = new Color(251,146,60);
    static final Color EMPTY   = new Color(30,36,54);

    static final Color[] PROC_COLORS = {
            new Color(99,179,237), new Color(72,235,180), new Color(167,139,250),
            new Color(253,224,132), new Color(134,239,172), new Color(249,168,212),
            new Color(147,197,253), new Color(252,165,165)
    };
    static Color procColor(int procId){ return PROC_COLORS[(procId-1)%PROC_COLORS.length]; }
}

// ══════════════════════════════════════════════════════════════
//  WIDGETS
// ══════════════════════════════════════════════════════════════

class DarkField extends JTextField {
    DarkField(int cols){
        super(cols);
        setBackground(new Color(10,12,20));
        setForeground(Pal.TEXT);
        setCaretColor(Pal.ACCENT);
        setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Pal.BORDER,1),new EmptyBorder(7,11,7,11)));
        setPreferredSize(new Dimension(90,36));
    }
    void markError(){ setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Pal.DANGER,1),new EmptyBorder(7,11,7,11))); }
    void markOk()   { setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Pal.BORDER,1),new EmptyBorder(7,11,7,11))); }
}

class PillButton extends JButton {
    private final Color base;
    PillButton(String txt, Color c){
        super(txt); this.base=c;
        setFont(new Font("Segoe UI",Font.BOLD,13));
        setForeground(Color.WHITE);
        setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
        setPreferredSize(new Dimension(130,38));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    @Override protected void paintComponent(Graphics g){
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.Color fill=getModel().isPressed()?base.darker():getModel().isRollover()?base.brighter():base;
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),12,12));
        g2.dispose(); super.paintComponent(g);
    }
}

// ══════════════════════════════════════════════════════════════
//  ALLOCATION VISUAL
// ══════════════════════════════════════════════════════════════

class AllocationVisual extends JPanel {
    Memory[]  memory;
    Process[] processes;
    String    strategy="";

    AllocationVisual(){ setBackground(Pal.CARD); }

    void update(Memory[] m, Process[] p, String s){ memory=m; processes=p; strategy=s; repaint(); }

    @Override public Dimension getPreferredSize(){
        int rows = (memory==null)?5:memory.length;
        return new Dimension(600, 48 + rows*64);
    }

    @Override protected void paintComponent(Graphics g){
        super.paintComponent(g);
        if (memory==null){ drawPlaceholder(g); return; }

        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int pad=18, labelW=52, rightGap=90;
        int blockH=48, gap=12;
        int barX=pad+labelW;
        int barW=getWidth()-barX-pad-rightGap;

        // header
        g2.setFont(new Font("Segoe UI",Font.BOLD,11));
        g2.setColor(Pal.MUTED);
        g2.drawString("MEMORY MAP  —  "+strategy, pad, 20);

        int y=32;
        for (Memory mem : memory){
            // bg track
            g2.setColor(Pal.EMPTY);
            g2.fillRoundRect(barX,y,barW,blockH,8,8);
            g2.setColor(Pal.BORDER);
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawRoundRect(barX,y,barW,blockH,8,8);

            if (mem.allocated){
                int procW=Math.max(1,(int)((double)mem.processSize/mem.size*barW));
                int fragW=barW-procW;
                Color pc=Pal.procColor(mem.processId);

                // process fill
                g2.setColor(pc);
                if (fragW>0) g2.fillRect(barX,y,procW,blockH);
                else         g2.fillRoundRect(barX,y,procW,blockH,8,8);

                // frag fill
                if (fragW>0){
                    g2.setColor(Pal.FRAG);
                    g2.fillRect(barX+procW,y,fragW,blockH);
                    // re-round right edge
                    g2.setColor(Pal.CARD);
                    g2.fillRoundRect(barX+barW-8,y,8,blockH,8,8);
                    g2.setColor(Pal.FRAG);
                    g2.fillRect(barX+barW-8,y,4,blockH);
                }

                // re-draw border on top
                g2.setColor(Pal.BORDER);
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRoundRect(barX,y,barW,blockH,8,8);

                // process label
                g2.setFont(new Font("Segoe UI",Font.BOLD,12));
                Color dark=new Color(Math.max(0,pc.getRed()-80),Math.max(0,pc.getGreen()-80),Math.max(0,pc.getBlue()-80));
                g2.setColor(dark);
                String pl="P"+mem.processId+"  "+mem.processSize+" KB";
                FontMetrics fm=g2.getFontMetrics();
                if (fm.stringWidth(pl)<procW-12)
                    g2.drawString(pl,barX+8,y+blockH/2+5);

                // frag label
                int fragSz=mem.size-mem.processSize;
                if (fragSz>0 && fragW>28){
                    g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
                    g2.setColor(new Color(255,255,255,180));
                    g2.drawString("+"+fragSz,barX+procW+5,y+blockH/2+5);
                }
            } else {
                g2.setFont(new Font("Segoe UI",Font.ITALIC,12));
                g2.setColor(Pal.MUTED);
                g2.drawString("free",barX+10,y+blockH/2+5);
            }

            // left: block id
            g2.setFont(new Font("Segoe UI",Font.BOLD,12));
            g2.setColor(Pal.MUTED);
            g2.drawString("B"+mem.id, pad, y+blockH/2+5);

            // right: block size
            g2.setFont(new Font("Segoe UI",Font.PLAIN,11));
            g2.setColor(Pal.MUTED);
            g2.drawString(mem.size+" KB", barX+barW+6, y+blockH/2+5);

            y+=blockH+gap;
        }

        g2.dispose();
    }

    private void drawPlaceholder(Graphics g){
        Graphics2D g2=(Graphics2D)g.create();
        g2.setFont(new Font("Segoe UI",Font.PLAIN,13));
        g2.setColor(Pal.MUTED);
        String s="Run a strategy to see the memory map";
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(s,(getWidth()-fm.stringWidth(s))/2,getHeight()/2);
        g2.dispose();
    }
}

// ══════════════════════════════════════════════════════════════
//  STATS CARDS
// ══════════════════════════════════════════════════════════════

class StatsPanel extends JPanel {
    private int iF=-1,eF=-1,al=-1,tot=-1;
    StatsPanel(){ setBackground(Pal.BG); setLayout(new GridLayout(1,4,8,0)); setPreferredSize(new Dimension(600,72)); refresh(); }
    void update(int i,int e,int a,int t){ iF=i;eF=e;al=a;tot=t; refresh(); }
    private void refresh(){
        removeAll();
        add(card("Internal frag", iF<0?"—":iF+" KB",  Pal.FRAG));
        add(card("External frag", eF<0?"—":eF+" KB",  Pal.DANGER));
        add(card("Allocated",     al<0?"—":al+" KB",  Pal.SUCCESS));
        add(card("Total memory",  tot<0?"—":tot+" KB", Pal.ACCENT));
        revalidate(); repaint();
    }
    private JPanel card(String lbl, String val, Color accent){
        JPanel p=new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Pal.CARD);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g2.setColor(accent);
                g2.fillRoundRect(0,getHeight()-3,getWidth(),3,2,2);
                g2.dispose();
            }
        };
        p.setOpaque(false); p.setBorder(new EmptyBorder(10,14,10,14));
        JLabel l=new JLabel(lbl); l.setFont(new Font("Segoe UI",Font.PLAIN,11)); l.setForeground(Pal.MUTED);
        JLabel v=new JLabel(val); v.setFont(new Font("Segoe UI",Font.BOLD,18)); v.setForeground(accent);
        p.add(l,BorderLayout.NORTH); p.add(v,BorderLayout.CENTER);
        return p;
    }
}

// ══════════════════════════════════════════════════════════════
//  SUMMARY TABLE
// ══════════════════════════════════════════════════════════════

class SummaryPanel extends JPanel {
    private final JPanel rows;
    SummaryPanel(){
        setBackground(Pal.CARD); setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12,16,12,16));
        JLabel t=new JLabel("COMPARISON SUMMARY");
        t.setFont(new Font("Segoe UI",Font.BOLD,11)); t.setForeground(Pal.MUTED);
        add(t,BorderLayout.NORTH);
        rows=new JPanel(); rows.setBackground(Pal.CARD); rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));
        rows.setBorder(new EmptyBorder(8,0,0,0));
        add(rows,BorderLayout.CENTER);
    }
    void addResult(String strat,int i,int e,Color c){
        JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,10,2)); row.setBackground(Pal.CARD);
        JLabel sl=new JLabel(strat); sl.setFont(new Font("Segoe UI",Font.BOLD,12)); sl.setForeground(c); sl.setPreferredSize(new Dimension(88,20));
        row.add(sl); chip(row,"INT "+i+"KB",Pal.FRAG); chip(row,"EXT "+e+"KB",Pal.DANGER); chip(row,"TOT "+(i+e)+"KB",c);
        rows.add(row); rows.revalidate(); rows.repaint();
    }
    void markBest(String strat){
        for (Component c:rows.getComponents()) if (c instanceof JPanel) {
            JPanel row=(JPanel)c;
            for (Component cc:row.getComponents()) if (cc instanceof JLabel && ((JLabel)cc).getText().equals(strat)){
                row.setBorder(BorderFactory.createLineBorder(Pal.SUCCESS,1));
                JLabel s=new JLabel(" ★ best"); s.setFont(new Font("Segoe UI",Font.BOLD,11)); s.setForeground(Pal.SUCCESS);
                row.add(s);
            }
        }
        revalidate(); repaint();
    }
    void clear(){ rows.removeAll(); rows.revalidate(); rows.repaint(); }
    private void chip(JPanel p,String txt,Color c){
        JLabel l=new JLabel(txt){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),45));
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),8,8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        l.setOpaque(false); l.setFont(new Font("Segoe UI",Font.BOLD,11)); l.setForeground(c);
        l.setBorder(new EmptyBorder(3,8,3,8)); p.add(l);
    }
}

// ══════════════════════════════════════════════════════════════
//  LEGEND PANEL
// ══════════════════════════════════════════════════════════════

class LegendPanel extends JPanel {
    Process[] processes;
    LegendPanel(){ setBackground(Pal.CARD); setPreferredSize(new Dimension(160,300)); }
    void setProcesses(Process[] p){ this.processes=p; repaint(); }
    @Override protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        int x=14,y=18;
        g2.setFont(new Font("Segoe UI",Font.BOLD,11));
        g2.setColor(Pal.MUTED);
        g2.drawString("LEGEND",x,y); y+=20;

        if (processes!=null) for (Process pr:processes){
            Color pc=Pal.procColor(pr.id);
            g2.setColor(pc);
            g2.fillRoundRect(x,y-11,12,12,4,4);
            g2.setFont(new Font("Segoe UI",Font.PLAIN,12));
            g2.setColor(Pal.TEXT);
            g2.drawString("P"+pr.id+"  "+pr.size+" KB",x+18,y);
            y+=18;
        }
        y+=6;
        g2.setColor(Pal.FRAG); g2.fillRoundRect(x,y-11,12,12,4,4);
        g2.setFont(new Font("Segoe UI",Font.PLAIN,12)); g2.setColor(Pal.TEXT);
        g2.drawString("Int. frag",x+18,y); y+=18;

        g2.setColor(Pal.EMPTY); g2.fillRoundRect(x,y-11,12,12,4,4);
        g2.setColor(Pal.BORDER); g2.setStroke(new BasicStroke(0.8f)); g2.drawRoundRect(x,y-11,12,12,4,4);
        g2.setFont(new Font("Segoe UI",Font.PLAIN,12)); g2.setColor(Pal.TEXT);
        g2.drawString("Free",x+18,y);

        g2.dispose();
    }
}

// ══════════════════════════════════════════════════════════════
//  INPUT WIZARD
// ══════════════════════════════════════════════════════════════

class InputWizard extends JPanel {
    interface DoneListener { void onDone(Memory[] mem, Process[] proc); }

    private int numBlocks,numProcs;
    private DarkField[] blockFields,procFields;
    private final CardLayout cards=new CardLayout();
    private final JPanel deck=new JPanel(cards);
    private final DoneListener listener;

    InputWizard(DoneListener dl){
        this.listener=dl;
        setBackground(Pal.CARD);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16,16,16,16));
        deck.setBackground(Pal.CARD);
        add(deck,BorderLayout.CENTER);
        showStep0();
    }

    private void showStep0(){
        JPanel p=new JPanel(new GridBagLayout()); p.setBackground(Pal.CARD);
        GridBagConstraints gc=new GridBagConstraints(); gc.insets=new Insets(8,6,8,6); gc.anchor=GridBagConstraints.WEST;

        DarkField bf=new DarkField(4), pf=new DarkField(4);

        JLabel h=lbl("Setup"); h.setFont(new Font("Segoe UI",Font.BOLD,14)); h.setForeground(Pal.ACCENT);
        gc.gridx=0;gc.gridy=0;gc.gridwidth=2; p.add(h,gc); gc.gridwidth=1;

        gc.gridx=0;gc.gridy=1; p.add(lbl("# of blocks"),gc);
        gc.gridx=1; p.add(bf,gc);
        gc.gridx=0;gc.gridy=2; p.add(lbl("# of processes"),gc);
        gc.gridx=1; p.add(pf,gc);

        PillButton next=new PillButton("NEXT →",Pal.ACCENT);
        gc.gridx=0;gc.gridy=3;gc.gridwidth=2;gc.anchor=GridBagConstraints.CENTER; p.add(next,gc);

        next.addActionListener(e->{
            try {
                int b=Integer.parseInt(bf.getText().trim()), pr=Integer.parseInt(pf.getText().trim());
                if(b<1||pr<1||b>16||pr>16) throw new NumberFormatException();
                numBlocks=b; numProcs=pr; bf.markOk(); pf.markOk(); showStep1();
            } catch(NumberFormatException ex){ bf.markError(); pf.markError(); }
        });
        deck.add(p,"s0"); cards.show(deck,"s0");
    }

    private void showStep1(){
        blockFields=new DarkField[numBlocks];
        JPanel p=new JPanel(new GridBagLayout()); p.setBackground(Pal.CARD);
        GridBagConstraints gc=new GridBagConstraints(); gc.insets=new Insets(5,6,5,6); gc.anchor=GridBagConstraints.WEST;

        JLabel h=lbl("Block sizes (KB)"); h.setFont(new Font("Segoe UI",Font.BOLD,13)); h.setForeground(Pal.ACCENT);
        gc.gridx=0;gc.gridy=0;gc.gridwidth=2; p.add(h,gc); gc.gridwidth=1;

        for(int i=0;i<numBlocks;i++){
            blockFields[i]=new DarkField(6);
            gc.gridx=0;gc.gridy=i+1; p.add(lbl("Block "+(i+1)),gc);
            gc.gridx=1; p.add(blockFields[i],gc);
        }

        JPanel btns=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); btns.setBackground(Pal.CARD);
        PillButton back=new PillButton("← BACK",new Color(50,60,85));
        PillButton next=new PillButton("NEXT →",Pal.ACCENT);
        btns.add(back); btns.add(next);
        gc.gridx=0;gc.gridy=numBlocks+1;gc.gridwidth=2; p.add(btns,gc);

        back.addActionListener(e->cards.show(deck,"s0"));
        next.addActionListener(e->{ if(validate(blockFields)) showStep2(); });

        JScrollPane sp=scroll(p); deck.add(sp,"s1"); cards.show(deck,"s1");
    }

    private void showStep2(){
        procFields=new DarkField[numProcs];
        JPanel p=new JPanel(new GridBagLayout()); p.setBackground(Pal.CARD);
        GridBagConstraints gc=new GridBagConstraints(); gc.insets=new Insets(5,6,5,6); gc.anchor=GridBagConstraints.WEST;

        JLabel h=lbl("Process sizes (KB)"); h.setFont(new Font("Segoe UI",Font.BOLD,13)); h.setForeground(Pal.SUCCESS);
        gc.gridx=0;gc.gridy=0;gc.gridwidth=2; p.add(h,gc); gc.gridwidth=1;

        for(int i=0;i<numProcs;i++){
            procFields[i]=new DarkField(6);
            gc.gridx=0;gc.gridy=i+1;
            JLabel pl=lbl("Process "+(i+1)); pl.setForeground(Pal.procColor(i+1));
            p.add(pl,gc); gc.gridx=1; p.add(procFields[i],gc);
        }

        JPanel btns=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); btns.setBackground(Pal.CARD);
        PillButton back=new PillButton("← BACK",new Color(50,60,85));
        PillButton done=new PillButton("✓ CONFIRM",Pal.SUCCESS);
        btns.add(back); btns.add(done);
        gc.gridx=0;gc.gridy=numProcs+1;gc.gridwidth=2; p.add(btns,gc);

        back.addActionListener(e->cards.show(deck,"s1"));
        done.addActionListener(e->{
            if(!validate(procFields)) return;
            Memory[] mem=new Memory[numBlocks];
            Process[] proc=new Process[numProcs];
            for(int i=0;i<numBlocks;i++) mem[i]=new Memory(i+1,Integer.parseInt(blockFields[i].getText().trim()));
            for(int i=0;i<numProcs;i++) proc[i]=new Process(i+1,Integer.parseInt(procFields[i].getText().trim()));
            listener.onDone(mem,proc);
        });

        JScrollPane sp=scroll(p); deck.add(sp,"s2"); cards.show(deck,"s2");
    }

    private boolean validate(DarkField[] fields){
        boolean ok=true;
        for(DarkField f:fields){
            try{ int v=Integer.parseInt(f.getText().trim()); if(v<=0)throw new NumberFormatException(); f.markOk(); }
            catch(NumberFormatException ex){ f.markError(); ok=false; }
        }
        return ok;
    }

    private JLabel lbl(String t){ JLabel l=new JLabel(t); l.setFont(new Font("Segoe UI",Font.PLAIN,13)); l.setForeground(Pal.TEXT); return l; }

    private JScrollPane scroll(JPanel p){
        JScrollPane sp=new JScrollPane(p); sp.setBackground(Pal.CARD); sp.getViewport().setBackground(Pal.CARD); sp.setBorder(null); return sp;
    }

    void reset(){ deck.removeAll(); showStep0(); }
}

// ══════════════════════════════════════════════════════════════
//  MAIN
// ══════════════════════════════════════════════════════════════

public class MemoryAllocationGUI {

    static Memory[]  currentMem;
    static Process[] currentProc;
    static AllocationVisual visual;
    static StatsPanel stats;
    static SummaryPanel summary;
    static LegendPanel legend;
    static JLabel statusBar;
    static final Map<String,int[]> results=new LinkedHashMap<>();

    public static void main(String[] args){
        SwingUtilities.invokeLater(MemoryAllocationGUI::build);
    }

    static void build(){
        JFrame frame=new JFrame("Memory Allocation Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1080,700);
        frame.setMinimumSize(new Dimension(920,600));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(Pal.BG);
        frame.setLayout(new BorderLayout(0,0));

        // header
        JPanel header=new JPanel(new BorderLayout()); header.setBackground(Pal.CARD);
        header.setBorder(new EmptyBorder(13,20,13,20));
        JLabel title=new JLabel("MEMORY ALLOCATION SIMULATOR"); title.setFont(new Font("Segoe UI",Font.BOLD,17)); title.setForeground(Pal.ACCENT);
        JLabel sub=new JLabel("First Fit  ·  Best Fit  ·  Worst Fit"); sub.setFont(new Font("Segoe UI",Font.PLAIN,12)); sub.setForeground(Pal.MUTED);
        header.add(title,BorderLayout.WEST); header.add(sub,BorderLayout.EAST);
        frame.add(header,BorderLayout.NORTH);

        // wizard (left)
        InputWizard wizard=new InputWizard((mem,proc)->{
            currentMem=mem; currentProc=proc;
            legend.setProcesses(proc);
            MemoryAllocationGUI.status("Data confirmed — choose a strategy below.",Pal.SUCCESS);
        });
        wizard.setPreferredSize(new Dimension(265,0));
        wizard.setBorder(BorderFactory.createMatteBorder(0,0,0,1,Pal.BORDER));

        // centre: stats + visual
        visual=new AllocationVisual();
        stats=new StatsPanel();
        legend=new LegendPanel();

        JScrollPane vizScroll=new JScrollPane(visual);
        vizScroll.setBorder(null);
        vizScroll.getViewport().setBackground(Pal.CARD);
        vizScroll.setBackground(Pal.CARD);

        JPanel vizRow=new JPanel(new BorderLayout(8,0)); vizRow.setBackground(Pal.CARD);
        vizRow.add(vizScroll,BorderLayout.CENTER);
        vizRow.add(legend,BorderLayout.EAST);

        summary=new SummaryPanel();
        summary.setPreferredSize(new Dimension(600,120));

        JPanel centre=new JPanel(new BorderLayout(0,10)); centre.setBackground(Pal.BG);
        centre.setBorder(new EmptyBorder(12,12,0,12));
        centre.add(stats,BorderLayout.NORTH);
        centre.add(vizRow,BorderLayout.CENTER);
        centre.add(summary,BorderLayout.SOUTH);

        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,wizard,centre);
        split.setDividerLocation(270); split.setDividerSize(1); split.setBorder(null);
        frame.add(split,BorderLayout.CENTER);

        // button bar (bottom)
        JPanel btnBar=new JPanel(new FlowLayout(FlowLayout.CENTER,10,8)); btnBar.setBackground(Pal.CARD);
        btnBar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Pal.BORDER));

        PillButton bFirst  =new PillButton("FIRST FIT", new Color(59,130,246));
        PillButton bBest   =new PillButton("BEST FIT",  new Color(16,185,129));
        PillButton bWorst  =new PillButton("WORST FIT", new Color(245,101,50));
        PillButton bSummary=new PillButton("SUMMARY",   new Color(139,92,246));
        PillButton bReset  =new PillButton("RESET",     new Color(60,65,85));

        btnBar.add(bFirst); btnBar.add(bBest); btnBar.add(bWorst); btnBar.add(bSummary); btnBar.add(bReset);

        statusBar=new JLabel("  Complete the wizard on the left, then run a strategy.");
        statusBar.setFont(new Font("Segoe UI",Font.PLAIN,12)); statusBar.setForeground(Pal.MUTED);

        JPanel bottom=new JPanel(new BorderLayout()); bottom.setBackground(Pal.CARD);
        bottom.add(btnBar,BorderLayout.CENTER); bottom.add(statusBar,BorderLayout.SOUTH);
        frame.add(bottom,BorderLayout.SOUTH);

        // listeners
        bFirst .addActionListener(e->run("FIRST","FIRST FIT", new Color(59,130,246)));
        bBest  .addActionListener(e->run("BEST", "BEST FIT",  new Color(16,185,129)));
        bWorst .addActionListener(e->run("WORST","WORST FIT", new Color(245,101,50)));

        bSummary.addActionListener(e->{
            if(results.isEmpty()){MemoryAllocationGUI.status("Run at least one strategy first.",Pal.WARN);return;}
            summary.clear();
            int best=Integer.MAX_VALUE; String bestKey="";
            for(Map.Entry<String,int[]> en:results.entrySet()){ int t=en.getValue()[0]+en.getValue()[1]; if(t<best){best=t;bestKey=en.getKey();} }
            for(Map.Entry<String,int[]> en:results.entrySet()){
                Color c; String k=en.getKey();
                if(k.equals("FIRST FIT"))c=new Color(59,130,246);
                else if(k.equals("BEST FIT"))c=new Color(16,185,129);
                else c=new Color(245,101,50);
                summary.addResult(k,en.getValue()[0],en.getValue()[1],c);
            }
            summary.markBest(bestKey);
            MemoryAllocationGUI.status("Summary updated.",Pal.SUCCESS);
        });

        bReset.addActionListener(e->{
            currentMem=null; currentProc=null; results.clear();
            visual.update(null,null,""); stats.update(-1,-1,-1,-1);
            summary.clear(); legend.setProcesses(null); wizard.reset();
            MemoryAllocationGUI.status("Reset. Enter new data.",Pal.MUTED);
        });

        frame.setVisible(true);
    }

    static void run(String strategy,String label,Color c){
        if(currentMem==null||currentProc==null){MemoryAllocationGUI.status("Confirm input data first (finish the wizard).",Pal.WARN);return;}
        Memory[]  m=copyM(currentMem);
        Process[] p=copyP(currentProc);
        MemoryAllocator.allocate(m,p,strategy);

        int intF=0,alloc=0,tot=0;
        for(Memory mem:m){ tot+=mem.size; if(mem.allocated){intF+=mem.size-mem.processSize;alloc+=mem.processSize;} }
        int extF=MemoryAllocator.externalFrag(m,copyP(currentProc));
        results.put(label,new int[]{intF,extF});

        switch(strategy){
            case"FIRST":MemoryAllocator.firstInternal=intF;MemoryAllocator.firstExternal=extF;break;
            case"BEST": MemoryAllocator.bestInternal =intF;MemoryAllocator.bestExternal =extF;break;
            case"WORST":MemoryAllocator.worstInternal=intF;MemoryAllocator.worstExternal=extF;break;
        }

        visual.update(m,p,label);
        stats.update(intF,extF,alloc,tot);
        status(label+"  |  Int frag = "+intF+" KB   Ext frag = "+extF+" KB",c);
    }

    static Memory[]  copyM(Memory[]  s){Memory[]  c=new Memory[s.length];  for(int i=0;i<s.length;i++)c[i]=new Memory(s[i].id,s[i].size);  return c;}
    static Process[] copyP(Process[] s){Process[] c=new Process[s.length]; for(int i=0;i<s.length;i++)c[i]=new Process(s[i].id,s[i].size); return c;}
    static void status(String msg, Color c){
        statusBar.setText("  " + msg);
        statusBar.setForeground(c);
    }
}
