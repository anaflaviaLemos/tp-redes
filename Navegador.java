import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class Navegador {

	private static int PORTA_PADRAO = 80;

	public static void main(String[] args) {

		String arquivo = "";
		int porta = PORTA_PADRAO;
		
		try {
			
			if ( args.length >= 1 ) arquivo = args[0];
			else throw new Exception();
			
			if ( args.length >= 2 ) porta = Integer.parseInt(args[1]);

			Navegador navegador = new Navegador();
			navegador.requisitar(arquivo, porta);
		
		} catch (NumberFormatException e) {
			System.out.println("Porta não é um número.");	
		} catch (Exception e) {
			System.out.println("URL em branco.");
		}
	}

	public void requisitar(String arquivo, int porta) {
		System.out.println("Arquivo - " + arquivo);
		System.out.println("Porta - " + porta);
		
		try {
			Socket cliente = new Socket("127.0.0.1", porta);
			
			DataOutputStream requisicao = new DataOutputStream(
					new BufferedOutputStream(cliente.getOutputStream()));
			
			enviarRequisicao(requisicao, arquivo);
			receberResposta(cliente.getInputStream(), arquivo);
			
			requisicao.close();
			cliente.close();
		} catch(UnknownHostException e) { 
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String construirHeader(String arquivo, String host) {
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append("GET /" + arquivo + "/ HTTP/1.1\r\n");
		stringBuilder.append("Host: " + host + "\r\n");
		stringBuilder.append("Connection: Close\r\n");
		
		return stringBuilder.toString();
	}
	
	public void enviarRequisicao(DataOutputStream requisicao, String arquivo) throws IOException {
		requisicao.writeUTF(construirHeader(arquivo, "127.0.0.1"));
		requisicao.flush();
	}
	
	public void receberResposta(InputStream resposta, String arquivo) throws IOException {
		System.out.println("---------------");
		
		DataInputStream respostaHeader = new DataInputStream(new BufferedInputStream(resposta));
		String header = respostaHeader.readUTF();
		
		if ( processarCodigoHeader(header) == 200 ) {
			System.out.println("Status 200 - OK");
			
			if ( processarTipoArquivoHeader(header).equals("text/html") ) {
				DataInputStream respostaArquivoHTML = new DataInputStream(new BufferedInputStream(resposta));
				salvarArquivoHTML(respostaArquivoHTML.readUTF(), arquivo);
			} else {
				BufferedImage bufferedImage = ImageIO.read(resposta);
				salvarArquivoImg(bufferedImage, arquivo);
			}
			
		} else {
			System.out.println("Erro 404 - Arquivo não encontrado.");
		}
	}
	
	private String processarTipoArquivoHeader(String respostaHeader) {
		return "";
	}
	
	private int processarCodigoHeader(String respostaHeader) {
		
		if ( respostaHeader.contains("200") ) {
			return 200;
		}
		
		return 404;
	}
	
	private void salvarArquivoImg(BufferedImage conteudo, String nome) {
		
		try {
			Pattern p = Pattern.compile("(https?://)([^:^/]*)(.*)?");
			Matcher m = p.matcher(nome);
			m.find();
			
			File arquivo = new File("cliente/" + m.group(3).replaceFirst("/", ""));
			
			String ext = arquivo.getName().substring(arquivo.getName().lastIndexOf(".") + 1,arquivo.getName().length());
			ImageIO.write(conteudo, ext, arquivo);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void salvarArquivoHTML(String conteudo, String nome) {
		try {
			Pattern p = Pattern.compile("(https?://)([^:^/]*)(.*)?");
			Matcher m = p.matcher(nome);
			m.find();
			
			FileWriter arquivo = new FileWriter("cliente/" + m.group(3).replaceFirst("/", ""));
			PrintWriter gravarArquivo = new PrintWriter(arquivo);
		
			gravarArquivo.append(conteudo);			
			gravarArquivo.flush();
			
			arquivo.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
