package sample;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

public class Main extends Application {

    public double values[][];
    public double permeability[];
    public double cValue = 6;
    
    int noOfParameter;
    public int noOfData=0;
    String depthUnit = "";
    double startDepth;
    double endDepth;
    double minPer;
    double maxPer;
    double nullValue;

    public int bfvIndex;
    public int cmffIndex;
    public int cmrpIndex;

    public Label fileError;
    public Label cValueError;
    public BorderPane layout;


    @Override
    public void start(Stage primaryStage) throws Exception{

        primaryStage.setTitle("Permeability estimation using NMR log - Siddharth's Dissertation");

        Button loadLas = new Button("Load file");
        loadLas.setOnAction(e->{
            FileChooser loadlasdirrctory = new FileChooser();
            loadlasdirrctory.getExtensionFilters().add(new FileChooser.ExtensionFilter("LAS Files", "*.las"));
            loadlasdirrctory.setTitle("Load LAS file for NMR estimation ");
            File selectedlas =  loadlasdirrctory.showOpenDialog(primaryStage);
            if(selectedlas != null){
                try {
                    boolean fileLoaded = loadlas(selectedlas);
                    if(fileLoaded) {
                        updateGraph();
                        primaryStage.setTitle("Permeability estimation of "+selectedlas.getName());
                    }
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
                if(newC<5 || newC>7)
                    handleError("c error", "c value should be in range [5,7]");
                else {
                    cValue = newC;
                    updateGraph();
                }
            }
            catch (Exception e){
                handleError("c error", "Invalid entry");
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

        primaryStage.setScene(new Scene(layout, 600, 400));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }

    public void updateGraph(){
        layout.setCenter(null);
        minPer = POSITIVE_INFINITY;
        maxPer = NEGATIVE_INFINITY;

        for (int i=0;i<noOfData;++i){
            if(values[i][1]==nullValue || values[i][2]==nullValue || values[i][3]==nullValue)
                permeability[i] = NEGATIVE_INFINITY;
            else{
                permeability[i] = (values[i][3]*values[i][3]*values[i][2])/(cValue*cValue*values[i][1]);
                permeability[i] = permeability[i]*permeability[i]*1000;
            }

            if(Double.isNaN(permeability[i]) || permeability[i] == POSITIVE_INFINITY || permeability[i] == NEGATIVE_INFINITY ){
            }
            else{
                if(maxPer < permeability[i])
                    maxPer = permeability[i];
                if(minPer > permeability[i])
                    minPer = permeability[i];
            }
        }
        maxPer *=1.2;
        minPer *=0.8;

        NumberAxis xAxis = new NumberAxis(minPer,maxPer,(maxPer-minPer)/5);
        xAxis.setLabel("Permeability of Coates in MD");
        xAxis.setAutoRanging(false);

        //Defining the y axis
        NumberAxis yAxis = new NumberAxis(endDepth,startDepth,-100);
        yAxis.setAutoRanging(false);
        yAxis.setLabel("Depth in "+depthUnit);

        //Creating the line chart
        LineChart linechart = new LineChart(xAxis, yAxis);

        linechart.setAnimated(false);
        linechart.setCreateSymbols(false);

        //Prepare XYChart.Series objects by setting data
        XYChart.Series series = new XYChart.Series();
        series.setName("Free Fluid (Timur Coater Model)");

        for (int j=0;j<=noOfData;++j){
            if(!Double.isNaN(permeability[j]) && permeability[j] != NEGATIVE_INFINITY && permeability[j] != POSITIVE_INFINITY)
                series.getData().add(new XYChart.Data(permeability[j],values[j][0]));
        }
        linechart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

        linechart.getData().add(series);
        series.getNode().setStyle("-fx-stroke-width: 2;-fx-stroke: blue; ");

        ScrollPane chart = new ScrollPane(linechart);
        linechart.setPrefHeight(layout.getHeight()*50-70);
        linechart.setPrefWidth(layout.getWidth()*-50);
        layout.heightProperty().addListener(e-> linechart.setPrefHeight(layout.getHeight()*50-70) );
        layout.widthProperty().addListener(e-> linechart.setPrefWidth(layout.getWidth()-50) );

        layout.setCenter(chart);
    }

    public boolean loadlas(File selectedlas)throws IOException{
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(selectedlas));

            String text;
            Pattern regex = Pattern.compile("(-?\\d+(?:\\.\\d+)?)"); //regex used as delimiter
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
//                        System.out.println(textInd+" "+Double.parseDouble(text.substring(textindex, indexOf)));
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
                        startDepth = Double.parseDouble(matcher.group(1));
                        noOfData -= startDepth;
                    }
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("STOP"))) {
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        endDepth = Double.parseDouble(matcher.group(1));
                        noOfData += endDepth;
                    }
                } else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("STEP"))) {
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        increment = Double.parseDouble(matcher.group(1));
                        noOfData = (int)Math.ceil(noOfData/increment);
                    }
                }else if (text.length() > 4 && (text.substring(0, 4).equalsIgnoreCase("NULL"))) {
                    Matcher matcher = regex.matcher(text);
                    while (matcher.find()) {
                        nullValue = Double.parseDouble(matcher.group(1));
                    }
                } else if (text.length() > 5 && (text.replaceAll("\\s", "").substring(0, 5).equalsIgnoreCase("depth"))) {
                    Isparameter = true;
                    noOfParameter = 1;
                    text = text.replaceAll("\\s", "");
//                    depthUnit = text.substring(text.indexOf(".")+1,text.indexOf(" ", text.indexOf(".")+1));
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
                    values = new double[noOfData + 10][4];
                    permeability = new double[noOfData + 10];
                    System.out.println("noOfParameter: "+noOfParameter);
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
