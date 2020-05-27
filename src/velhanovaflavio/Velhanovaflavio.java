/*

    FMU - SISTEMAS DISTRIBUIDOS
    Flavio Machado de Sousa - Ra: 6654670

*/

package velhanovaflavio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Velhanovaflavio implements Runnable {

	private String ip = "127.0.0.1";
	private int porta = 9000;
	private Scanner scanner = new Scanner(System.in);
	private JFrame tela;
	private final int LARGURA = 506;
	private final int ALTURA = 527;
	private Thread thread;

	private Painter pintar;
	private Socket socket;
	private DataOutputStream dos;
	private DataInputStream dis;

	private ServerSocket serverSocket;

	private BufferedImage Tabuleiro;
	private BufferedImage XVermelho;
	private BufferedImage XAzul;
	private BufferedImage BolinhaVermelha;
	private BufferedImage BolinhaVerde;

	private String[] espacos = new String[9];

	private boolean seuturno = false;
	private boolean circulo = true;
	private boolean aceitaram = false;
	private boolean incapazcomunicaroponente = false;
	private boolean ganhou = false;
	private boolean inimigovenceu = false;
	private boolean empate = false;

	private int comprimentoespaco = 160;
	private int erros = 0;
	private int primeiroponto = -1;
	private int segundoponto = -1;

	private Font fonte = new Font("Verdana", Font.BOLD, 32);
	private Font fontemenor = new Font("Verdana", Font.BOLD, 20);
	private Font fontemaior = new Font("Verdana", Font.BOLD, 50);

	private String esperando = "Esperando outro jogador";
	private String incapazcomunicacaooponente = "Nao foi possível se comunicar com o oponente.";
	private String ganhouaviso = "Você ganhou!";
	private String inimigoganhouaviso = "Oponente venceu!";
	private String empateaviso = "O jogo terminou empatado.";

	private int[][] vitoria = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };

	public Velhanovaflavio() {
		System.out.println("Por favor insira o IP: ");
		ip = scanner.nextLine();
		System.out.println("Por favor insira a porta: ");
		porta = scanner.nextInt();
		while (porta < 1 || porta > 65535) {
			System.out.println("A porta que você digitou é inválida. Insira outra porta: ");
			porta = scanner.nextInt();
		}

		loadimage();

		pintar = new Painter();
		pintar.setPreferredSize(new Dimension(LARGURA, ALTURA));

		if (!connect()) initializeServer();

		tela = new JFrame();
		tela.setTitle("Jogo Velha FMU");
		tela.setContentPane(pintar);
		tela.setSize(LARGURA, ALTURA);
		tela.setLocationRelativeTo(null);
		tela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		tela.setResizable(false);
		tela.setVisible(true);

		thread = new Thread(this, "Jogo Velha FMU");
		thread.start();
	}

	public void run() {
		while (true) {
			tick();
			pintar.repaint();
			if (!circulo && !aceitaram) {
				listenForServerRequest();
			}
		}
	}

	private void render(Graphics g) {
		g.drawImage(Tabuleiro, 0, 0, null);
		if (incapazcomunicaroponente) {
			g.setColor(Color.RED);
			g.setFont(fontemenor);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(incapazcomunicacaooponente);
			g.drawString(incapazcomunicacaooponente, LARGURA / 2 - stringWidth / 2, ALTURA / 2);
			return;
		}

		if (aceitaram) {
			for (int i = 0; i < espacos.length; i++) {
				if (espacos[i] != null) {
					if (espacos[i].equals("X")) {
						if (circulo) {
							g.drawImage(XVermelho, (i % 3) * comprimentoespaco + 10 * (i % 3), (int) (i / 3) * comprimentoespaco + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(XAzul, (i % 3) * comprimentoespaco + 10 * (i % 3), (int) (i / 3) * comprimentoespaco + 10 * (int) (i / 3), null);
						}
					} else if (espacos[i].equals("O")) {
						if (circulo) {
							g.drawImage(BolinhaVerde, (i % 3) * comprimentoespaco + 10 * (i % 3), (int) (i / 3) * comprimentoespaco + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(BolinhaVermelha, (i % 3) * comprimentoespaco + 10 * (i % 3), (int) (i / 3) * comprimentoespaco + 10 * (int) (i / 3), null);
						}
					}
				}
			}
			if (ganhou || inimigovenceu) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(10));
				g.setColor(Color.BLACK);
				g.drawLine(primeiroponto % 3 * comprimentoespaco + 10 * primeiroponto % 3 + comprimentoespaco / 2, (int) (primeiroponto / 3) * comprimentoespaco + 10 * (int) (primeiroponto / 3) + comprimentoespaco / 2, segundoponto % 3 * comprimentoespaco + 10 * segundoponto % 3 + comprimentoespaco / 2, (int) (segundoponto / 3) * comprimentoespaco + 10 * (int) (segundoponto / 3) + comprimentoespaco / 2);
				g.setColor(Color.RED);
				g.setFont(fontemaior);
				if (ganhou) {
					int stringWidth = g2.getFontMetrics().stringWidth(ganhouaviso);
					g.drawString(ganhouaviso, LARGURA / 2 - stringWidth / 2, ALTURA / 2);
				} else if (inimigovenceu) {
					int stringWidth = g2.getFontMetrics().stringWidth(inimigoganhouaviso);
					g.drawString(inimigoganhouaviso, LARGURA / 2 - stringWidth / 2, ALTURA / 2);
				}
			}
			if (empate) {
				Graphics2D g2 = (Graphics2D) g;
				g.setColor(Color.BLACK);
				g.setFont(fontemaior);
				int stringWidth = g2.getFontMetrics().stringWidth(empateaviso);
				g.drawString(empateaviso, LARGURA / 2 - stringWidth / 2, ALTURA / 2);
			}
		} else {
			g.setColor(Color.RED);
			g.setFont(fonte);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(esperando);
			g.drawString(esperando, LARGURA / 2 - stringWidth / 2, ALTURA / 2);
		}

	}

	private void tick() {
		if (erros >= 10) incapazcomunicaroponente = true;

		if (!seuturno && !incapazcomunicaroponente) {
			try {
				int space = dis.readInt();
				if (circulo) espacos[space] = "X";
				else espacos[space] = "O";
				checkEnemyWin();
				checkTie();
				seuturno = true;
			} catch (IOException e) {
				e.printStackTrace();
				erros++;
			}
		}
	}

	private void checkWin() {
		for (int i = 0; i < vitoria.length; i++) {
			if (circulo) {
				if (espacos[vitoria[i][0]] == "O" && espacos[vitoria[i][1]] == "O" && espacos[vitoria[i][2]] == "O") {
					primeiroponto = vitoria[i][0];
					segundoponto = vitoria[i][2];
					ganhou = true;
				}
			} else {
				if (espacos[vitoria[i][0]] == "X" && espacos[vitoria[i][1]] == "X" && espacos[vitoria[i][2]] == "X") {
					primeiroponto = vitoria[i][0];
					segundoponto = vitoria[i][2];
					ganhou = true;
				}
			}
		}
	}

	private void checkEnemyWin() {
		for (int i = 0; i < vitoria.length; i++) {
			if (circulo) {
				if (espacos[vitoria[i][0]] == "X" && espacos[vitoria[i][1]] == "X" && espacos[vitoria[i][2]] == "X") {
					primeiroponto = vitoria[i][0];
					segundoponto = vitoria[i][2];
					inimigovenceu = true;
				}
			} else {
				if (espacos[vitoria[i][0]] == "O" && espacos[vitoria[i][1]] == "O" && espacos[vitoria[i][2]] == "O") {
					primeiroponto = vitoria[i][0];
					segundoponto = vitoria[i][2];
					inimigovenceu = true;
				}
			}
		}
	}

	private void checkTie() {
		for (int i = 0; i < espacos.length; i++) {
			if (espacos[i] == null) {
				return;
			}
		}
		empate = true;
	}

	private void listenForServerRequest() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			aceitaram = true;
			System.out.println("O CLIENTE PEDIU PARA JUNTAR-SE E ACEITAMOS");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean connect() {
		try {
			socket = new Socket(ip, porta);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			aceitaram = true;
		} catch (IOException e) {
			System.out.println("Conectado ao endereço: " + ip + ":" + porta + " | Iniciando um servidor");
			return false;
		}
		System.out.println("Conectado com sucesso ao servidor.");
		return true;
	}

	private void initializeServer() {
		try {
			serverSocket = new ServerSocket(porta, 8, InetAddress.getByName(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		seuturno = true;
		circulo = false;
	}

	private void loadimage() {
		try {
			Tabuleiro = ImageIO.read(getClass().getResourceAsStream("res/Tabuleiro.png"));
			XVermelho = ImageIO.read(getClass().getResourceAsStream("res/XVermelho.png"));
			BolinhaVermelha = ImageIO.read(getClass().getResourceAsStream("res/BolinhaVermelha.png"));
			XAzul = ImageIO.read(getClass().getResourceAsStream("res/XAzul.png"));
			BolinhaVerde = ImageIO.read(getClass().getResourceAsStream("res/BolinhaVerde.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("não utilizado")
	public static void main(String[] args) {
		Velhanovaflavio velhanovaflavio = new Velhanovaflavio();
	}

	private class Painter extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;

		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (aceitaram) {
				if (seuturno && !incapazcomunicaroponente && !ganhou && !inimigovenceu) {
					int x = e.getX() / comprimentoespaco;
					int y = e.getY() / comprimentoespaco;
					y *= 3;
					int position = x + y;

					if (espacos[position] == null) {
						if (!circulo) espacos[position] = "X";
						else espacos[position] = "O";
						seuturno = false;
						repaint();
						Toolkit.getDefaultToolkit().sync();

						try {
							dos.writeInt(position);
							dos.flush();
						} catch (IOException e1) {
							erros++;
							e1.printStackTrace();
						}

						System.out.println("DADOS FORAM ENVIADOS");
						checkWin();
						checkTie();

					}
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {

		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

	}

}