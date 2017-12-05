package ru.company.client;
import com.sun.corba.se.impl.orbutil.threadpool.TimeoutException;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import sun.awt.SunHints;


import java.io.*;
import java.net.*;
import java.util.ResourceBundle;
import java.util.concurrent.*;

import static com.sun.xml.internal.messaging.saaj.packaging.mime.util.ASCIIUtility.getBytes;
import static java.lang.Thread.sleep;
public class Controller implements Initializable{
   private int [][] matrix1;
   private int [][] matrix2;
   private Socket socket;
   private ObjectOutputStream out;
   private ObjectInputStream in;
   public  String adress;
    @FXML
    TextArea textArea;
    @FXML
    TextField msgField;
    @FXML
    Button on_client;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        UDP_broadcast();
    }
    public void UDP_broadcast(){
        new Thread(()->{
        int port=2001;
        DatagramSocket ds;      //Класс - канал для передачи мусора в UDP протоколе
        byte[] b =getBytes("h");
        try {
            ds = new DatagramSocket();
            ds.setSoTimeout(2000);
            do {
                DatagramPacket d_send = new DatagramPacket(b, b.length, InetAddress.getByName("255.255.255.255"), port);
                ds.send(d_send);
                DatagramPacket dr = new DatagramPacket(b, b.length);
                try {
                    ds.receive(dr);  //получение
                    adress = String.valueOf(dr.getAddress());
                    //в адрес попадает "/", поэтомумы должны убрать последний символ
                    int start = 1;
                    int end = adress.length();
                    char[] dst = new char[end - start];
                    adress.getChars(start, end, dst, 0);
                    adress = String.valueOf(dst);
                }catch (SocketTimeoutException e){
                    continue;
                }
            }while(adress==null);
            setIP();    //вызовфункции
            ds.close(); //вызов функции (закрытие сокета)
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        }).start();
    }
    public void setIP(){
        new Thread(() -> {
                System.out.println(adress);
                if (adress != null) {
                    try {
                    socket = new Socket(adress, 7790);
                    in = new ObjectInputStream(socket.getInputStream());//getInputStream- получает поток ввода
                    out = new ObjectOutputStream(socket.getOutputStream());
                    textArea.appendText("Клиент " + socket.getLocalPort() + " подключен к серверу...\n");
                    int sizeMatrix;
                    while (true) {
                        sizeMatrix = in.readInt();
                        matrix1 = new int[sizeMatrix][sizeMatrix];
                        matrix2 = new int[sizeMatrix][sizeMatrix];
                        for (int i = 0; i < sizeMatrix; i++) {
                            for (int j = 0; j < sizeMatrix; j++) {
                                matrix1[i][j] = in.readInt();
                            }
                        }
                        for (int i = 0; i < sizeMatrix; i++) {
                            for (int j = 0; j < sizeMatrix; j++) {
                                matrix2[i][j] = in.readInt();
                            }
                        }

                        int index_i = in.readInt();
                        int index_j = in.readInt();
                        boolean flag_doing = in.readBoolean();
                        boolean busy = in.readBoolean();
                        int element = calcValue(index_i, index_j);
                        System.out.println(element);
                        out.writeInt(element);
                        out.flush();
                    }
                } catch(IOException e){
                    System.out.println("Клиент отключен и сокет закрыт!");
                }
            }
        }).start();
    }
    public int calcValue(final int row, final int col)
    {
        int sum = 0;
        for (int i = 0; i < matrix1.length; ++i)
            sum += matrix1[row][i] * matrix2[i][col];
        return sum;
    }
    public void sendMsg(){
        try {
                textArea.appendText("Клиент с портом "+ socket.getLocalPort()+ " оключен! \n");
                socket.close();
                return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
