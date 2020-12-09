/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controller;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.derby.jdbc.ClientDriver;



/**
 *
 * @author Amin
 */
public class ServerMainPageController implements Initializable {
    ServerSocket serverSocket ;
    Socket socket ;
    Connection con;
    ResultSet rs ;
    PreparedStatement pst;
    Thread listener;
    private boolean serverState ;
    
    @FXML
    private ImageView serverStateImage;
    @FXML
    private Label status;  
    @FXML
    private ScrollPane scrollpane;
    @FXML
    private Label currentLabel;
    @FXML
    private Button listOnlinebtn;
    @FXML
    private Button listOfflinebtn;
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        serverState = false;
        disableBtn();
    }
    
    @FXML
    private void toggleServer(ActionEvent event) throws InterruptedException{
        System.out.println("Toggle server");
        serverState = !serverState;
        if(serverState){ // state is false needed to be activate
            enableConnection();
        }else{ // state is true needed to be deactivate
            
            try {
                serverStateImage.setImage(new Image(new FileInputStream("src/resources/launch.png")));
                status.setText("Activate");
                currentLabel.setText("Status : OFF");
            }catch (FileNotFoundException ex) {
                System.out.println("No Img");
            }finally{
                emptyList();
                disableBtn();
                disableConnections();
            }
        }
            
        
    }
    
    @FXML
    private void listOnline(ActionEvent event) {
        listPlayers(true);
     
    }
    @FXML
    private void listOffline(ActionEvent event){
        listPlayers(false);
    }
 
 
    private void listPlayers(Boolean state){
        try {
            Button button;
            VBox vbox = new VBox();
            HBox hbox;
            while(rs.next()){
                if(rs.getString("isactive").equals(state+"")){
                    ImageView view,view2;
                    try {
                        // avatar view
                        view = new ImageView(new Image(new FileInputStream("src/resources/avatar.png")));
                        view.setFitHeight(30);
                        view.setPreserveRatio(true);

                        // active icon view
                        if(state)
                            view2 = new ImageView(new Image(new FileInputStream("src/resources/active.png")));
                        else
                            view2 = new ImageView(new Image(new FileInputStream("src/resources/inactive.png")));
                            

                        view2.setFitHeight(20);
                        view2.setPreserveRatio(true);

                        button = new Button(""+rs.getString("userName"),view);
                        button.setAlignment(Pos.BOTTOM_LEFT);

                        hbox = new HBox(button,view2);
                        HBox.setMargin(view2, new Insets(10,0,0,5)); // top right bottom left
                        button.getStyleClass().add("button1");
                        vbox.getChildren().add(hbox);

                        scrollpane.setContent(vbox);
                    } catch (FileNotFoundException ex) {
                        System.out.println("Image Not Found");
                    }
                }
            }
            rs.beforeFirst();
        } catch (SQLException ex) {
            Logger.getLogger(ServerMainPageController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void emptyList(){
        scrollpane.setContent(null);
    }
    private void disableBtn(){
        listOnlinebtn.setDisable(true);
        listOfflinebtn.setDisable(true);
    }
    private void enableBtn(){
        listOnlinebtn.setDisable(false);
        listOfflinebtn.setDisable(false);
    }
    
    private void enableConnection(){
        
        try{
            DriverManager.registerDriver(new ClientDriver());
            con = DriverManager.getConnection("jdbc:derby://localhost:1527/TicTackToy","root","root");
            updateResultSet();
            listPlayers(true);
            enableBtn();    // enable list online and offline btn;
            initServer(); // enable socket server
            
            Thread.sleep(200);
            serverStateImage.setImage(new Image(new FileInputStream("src/resources/shutdown.png")));
            status.setText("Deactivate");
            currentLabel.setText("Status : On");
                
        }catch(SQLException e){
            System.out.println("Connection Issues, Try again later");
            serverState = !serverState;
//            alert connection 
        }catch (FileNotFoundException ex) {
            System.out.println("loading image issues");
        }catch (InterruptedException ex) {
            
        }
    }
    
    private void updateResultSet(){
        try {
            pst = con.prepareStatement("Select * from player",ResultSet.TYPE_SCROLL_INSENSITIVE ,ResultSet.CONCUR_READ_ONLY );
            rs = pst.executeQuery(); // rs has all data
        }catch (SQLException ex) {
            System.out.println("Connection Issues, Try again later");
            //alert
        }
    }
    
    private void disableConnections(){
        try {
            rs.close();
            pst.close();
            con.close();
            listener.stop();
            serverSocket.close();
        } catch (SQLException ex) {
            //alert connection issue
        } catch (IOException ex) {
            Logger.getLogger(ServerMainPageController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void initServer(){
        try {
            serverSocket = new ServerSocket(9876);
            
            listener = new Thread(() -> {
                while(true){
                    try {
                        socket = serverSocket.accept();
                        new ConnectedPlayer(socket);
                    }catch (IOException ex) {
                        Logger.getLogger(ServerMainPageController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }
            });
            listener.start();
        }catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
}

class ConnectedPlayer{
   DataInputStream dis;
   PrintStream ps;
   static ArrayList<ConnectedPlayer> players = new ArrayList<ConnectedPlayer>();
   
   public ConnectedPlayer(Socket socket){
       try {
            dis = new DataInputStream(socket.getInputStream());
            ps = new PrintStream(socket.getOutputStream());
       }catch (IOException ex) {
            ex.printStackTrace();
       }
       players.add(this);
     
   }
}
