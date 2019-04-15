package servidor;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public class ServidorThread implements Runnable {
	
	private static int SUCESSO = 0;
	private static int ERRO = 1;
	
	private Socket conexao;
	private String pasta;
	
	public ServidorThread(Socket conexao, String pasta) {
		this.conexao = conexao;
		this.pasta = pasta;
	}

	@Override
	public void run() {
		
		try {
			File arquivo = receberRequisicao();	
			enviarRespostaSucesso(arquivo);			
		} catch (IOException e) {
			try {
				enviarRespostaErro();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} finally {
			try {
				this.conexao.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public BufferedImage lerArquivoImg(File arquivo) throws FileNotFoundException {
		
		BufferedImage bufferedImage = null;
		
		try {
			bufferedImage = ImageIO.read(arquivo);
		} catch (FileNotFoundException e ) {
			throw new FileNotFoundException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return bufferedImage;
	}
	
	public String lerArquivoHTML(File arquivo) throws FileNotFoundException {
		
		StringBuilder conteudo = new StringBuilder();
		
		try {
			FileReader leitor = new FileReader(arquivo);
			BufferedReader buffer = new BufferedReader(leitor);
			
			String linha = "";
			while ((linha = buffer.readLine()) != null) {
				conteudo.append(linha).append("\n");
			}			
		} catch (FileNotFoundException e ) {
			throw new FileNotFoundException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return conteudo.toString();
	}
	
	public String construirHeader(int status, String tipo) {
		StringBuilder stringBuilder = new StringBuilder();
		
		if ( status == SUCESSO) {
			stringBuilder.append("HTTP/1.1 200 OK\r\n");
			stringBuilder.append("Date: " + new SimpleDateFormat("dd/MM/yyyy h:mm:ss a").format(new Date()) + "\r\n");
			stringBuilder.append("Server: 127.0.0.1\r\n");
			stringBuilder.append("Content-Type: text/html\r\n");
			stringBuilder.append("Connection: Closed\r\n\r\n");
		} else if ( status == ERRO ) {
			stringBuilder.append("HTTP/1.1 404 Not Found\r\n");
			stringBuilder.append("Date: " + new SimpleDateFormat("dd/MM/yyyy h:mm:ss a").format(new Date()) + "\r\n");
			stringBuilder.append("Server: 127.0.0.1\r\n");
			stringBuilder.append("Connection: Closed\r\n\r\n");
		}
		
		return stringBuilder.toString();
	}
	
	public File receberRequisicao() throws IOException {
		
		DataInputStream requisicao = new DataInputStream(
				new BufferedInputStream(this.conexao.getInputStream()));
		
		String header = requisicao.readUTF();
		String url = "";
		
		String getLinha = header.substring(0, header.indexOf("\n"));
		url = getLinha.replace("GET /", "");
		url = url.replace("/ HTTP/1.1\r", "");
		
		Pattern p = Pattern.compile("(https?://)([^:^/]*)(.*)?");
		Matcher m = p.matcher(url);
		m.find();
		
		String caminho = m.group(3).replaceFirst("/", "");
		
		System.out.println(" --- Requisição: " + this.pasta + "/" + caminho);
				
		return new File(this.pasta + "/" + caminho);
	}
	
	public void enviarRespostaSucesso(File arquivo) throws FileNotFoundException, IOException {	
		String tipo = Files.probeContentType(arquivo.toPath());
		
		DataOutputStream respostaHeader = new DataOutputStream(new BufferedOutputStream(this.conexao.getOutputStream()));
		respostaHeader.writeUTF(construirHeader(SUCESSO,tipo));
		respostaHeader.flush();
		
		if ( tipo.equals("text/html") ) {
			DataOutputStream respostaArquivoHTML = new DataOutputStream(new BufferedOutputStream(this.conexao.getOutputStream()));
			respostaArquivoHTML.writeUTF(lerArquivoHTML(arquivo));
			respostaArquivoHTML.flush();
		} else if ( tipo.contains("image") ) {
			String ext = arquivo.getName().substring(arquivo.getName().lastIndexOf(".") + 1,arquivo.getName().length());
			ImageIO.write(lerArquivoImg(arquivo), ext, this.conexao.getOutputStream());
		}
	}
	
	public void enviarRespostaErro() throws IOException {
		DataOutputStream respostaHeader = new DataOutputStream(new BufferedOutputStream(this.conexao.getOutputStream()));
		respostaHeader.writeUTF(construirHeader(ERRO, null));
		respostaHeader.flush();
		respostaHeader.close();
	}
}
