package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {
	
	private static int PORTA_PADRAO = 80;

	public static void main(String args[]) {

		String pasta = "";
		int porta = PORTA_PADRAO;
		
		try {
			
			if ( args.length >= 1 ) pasta = args[0];
			else throw new Exception();
			
			if ( args.length >= 2 ) porta = Integer.parseInt(args[1]);

			Servidor servidor = new Servidor();
			servidor.iniciar(pasta,porta);
		
		} catch (NumberFormatException e) {
			System.out.println("Porta não é um número.");	
		} catch (Exception e) {
			System.out.println("Pasta em branco.");
		}
	}
	
	public void iniciar(String pasta, int porta) {
		
		try {
			ServerSocket socketServidor = new ServerSocket(porta);
			
			System.out.println("Servidor iniciado ...");
			System.out.println("------------------------------");
			System.out.println("Ouvindo na porta: " + porta);
			System.out.println("Pasta de trabalho: " + pasta);
			
			while ( true ) {
				Socket conexao = socketServidor.accept();
				new Thread(new ServidorThread(conexao, pasta)).start();
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
