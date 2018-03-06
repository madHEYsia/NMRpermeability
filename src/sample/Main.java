package sample;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    public double values[][];
    public double cValue = 6;
    
    int noOfParameter;
    public double noOfData=0.0;
    
    public int bfvIndex;
    public int cmffIndex;
    public int cmrpIndex;

    public Label fileError;
    public Label cValueError;
    public BorderPane layout;


    @Override
    public void start(Stage primaryStage) throws Exception{

        Button loadLas = new Button("Load file");
        loadLas.setOnAction(e->{
            FileChooser loadlasdirrctory = new FileChooser();
            loadlasdirrctory.getExtensionFilters().add(new FileChooser.ExtensionFilter("LAS Files", "*.las"));
            loadlasdirrctory.setTitle("Load LAS file for NMR estimation ");
            File selectedlas =  loadlasdirrctory.showOpenDialog(primaryStage);
            if(selectedlas != null){
                try {
                    boolean fileLoaded = loadlas(selectedlas);
                    if(fileLoaded)
                        updateGraph();
                }
                catch (IOException el){
                    el.printStackTrace();
                }
            }
            else {
                handleError("file", "file not laoded/ cancelled");
            }
        });
        fileError = new Label("");
        fileError.setFont(new Font("Arial", 11));
        fileError.setStyle("-fx-text-fill: red;");
        VBox fileHB = new VBox(5, loadLas, fileError);

        Label cValueLabel = new Label("'C' value");
        TextField cValueText = new TextField("6");
        cValueText.setPrefWidth(50);
        cValueText.textProperty().addListener((observable, oldValue, newValue) -> {
            try{
                double newC = Double.parseDouble(newValue);
                cValue = newC;
                if(newC<5 || newC>7)
                    handleError("c error", "c value should be in range [5,7]");
                else
                    updateGraph();
            }
            catch (Exception e){
                handleError("c error", "found internal error - "+e.getMessage().substring(0,50)+"  ......");
            }
        });
        cValueText.setPromptText("enter c value in range 5-7");
        cValueError = new Label("");
        cValueError.setFont(new Font("Arial", 11));
        cValueError.setStyle("-fx-text-fill: red;");
        VBox cValueHB = new VBox(5, new HBox(10,cValueLabel, cValueText), cValueError);

        layout = new BorderPane();
        layout.setPadding(new Insets(10));
        layout.setTop(new HBox(50,fileHB, cValueHB));

        primaryStage.setTitle("Permeability estimation using NMR log - Siddharth's Dissertation");
        primaryStage.setScene(new Scene(layout, 600, 400));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    public void updateGraph(){

    }

    public boolean loadlas(File selectedlas)throws IOException{
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(selectedlas));

            String text;
            Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?)"); //regex used as delimiter
            boolean Isdata = false, Isparameter = false;
            int index = 0;
            double increment;

            boolean bfv= false, cmff= false, cmrp = false;

            int textInd = 0;
            while ((text = bufferedReader.readLine()) != null && text.length() > 0) {
                if (Isdata) {
                    if(textInd==noOfParameter){
                        ++index;
                        textInd = 0;
                    }
                    //Replacing multiple line spaces to single space
                    text += " ";
                    text = (text.replaceAll("[ ]+", " ")).substring(1);
                    int textindex = 0;

                    //finding indexOF of spaces which gives me parameter value just before that
                    while (text.indexOf(" ", textindex) > 0) {
                        int indexOf = text.indexOf(" ", textindex);
                        if(textInd==0)
                            values[index][0] = Double.parseDouble(text.substring(textindex, indexOf));
                        else if(textInd==bfvIndex)
                            values[index][1] = Double.parseDouble(text.substring(textindex, indexOf));
                        else if(textInd==cmffIndex)
                            values[index][2] = Double.parseDouble(text.substring(textindex, indexOf));
                        else if(textInd==cmrpIndex)
                            values[index][3] = Double.parseDouble(text.substring(textindex, indexOf));
                        textindex = indexOf + 1;
                        ++textInd;
                    }
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("STRT"))) {
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        noOfData -= Double.parseDouble(matcher.group(1));
                    }
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("STOP"))) {
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        noOfData += Double.parseDouble(matcher.group(1));
                    }
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("STEP"))) {
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        increment = Double.parseDouble(matcher.group(1));
                        noOfData /= increment;
                    }
                } else if (text.length() > 5 && (text.replaceAll("\\s", "").substring(0, 5).equalsIgnoreCase("depth"))) {
                    Isparameter = true;
                    noOfParameter = 1;
                } else if (Isparameter && !(text.replaceAll("\\s", "").substring(0, 1).equalsIgnoreCase("~"))) {
                    ++noOfParameter;
                    String parameter = text.substring(0, text.replaceAll("\\s", "").indexOf("."));
                    if(!bfv && parameter.equals("BFV")){
                        bfv = true;
                        bfvIndex = noOfParameter-1;
                    }
                    else if(!cmff && parameter.equals("CMFF")){
                        cmff = true;
                        cmffIndex = noOfParameter-1;
                    }
                    else if(!cmrp && parameter.equals("CMRP_3MS")){
                        cmrp = true;
                        cmrpIndex = noOfParameter-1;
                    }
                }
                else if (text.length() > 4 && (text.replaceAll("\\s", "").substring(0, 2).equalsIgnoreCase("~A"))) {
                    if (!bfv){
                        handleError("file", "BFV data not found");
                        return false;
                    }
                    else if (!cmff){
                        handleError("file", "CMFF data not found");
                        return false;
                    }
                    else if (!cmrp){
                        handleError("file", "CMRP data not found");
                        return false;
                    }
                    Isdata = true;
                    values = new double[(int)Math.ceil(noOfData) + 1][4];
                }else{
                    Isparameter = false;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public void handleError(String errorLabel, String error){
        if (errorLabel.equals("file"))
            fileError.setText(error);
        else
            cValueError.setText(error);
        Task<Void> sleeper = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        if (errorLabel.equals("file"))
            sleeper.setOnSucceeded(event-> fileError.setText(""));
        else
            sleeper.setOnSucceeded(event-> cValueError.setText(""));

        new Thread(sleeper).start();
    }
}
