package com.dts.fel;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.dts.classes.clsFELInFile;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;


public class MainActivity extends PBase {

    private String fileName= Environment.getExternalStorageDirectory() + "/zzxmltest.xml";
    private String jsonName= Environment.getExternalStorageDirectory() + "/zzxmltest.txt";

    private clsFELInFile fel;

    private JSONObject jsonf = new JSONObject();
    private JSONObject jsonc = new JSONObject();
    private JSONObject jsona = new JSONObject();

    private String s64, jsfirm,jscert,jsanul,firma;

    private String  error;
    private Boolean errorflag;

    private String fel_llavews ="E5DC9FFBA5F3653E27DF2FC1DCAC824D";
    private String fel_token ="5b174fb0e23645b65ef88277d654603d";
    private String fel_codigo="0";
    private String fel_alias="DEMO_FEL";
    private String fel_ident="abc128"; // cambiar por cada documento

    private String fel_uuid="94564CED-39CD-477C-BB12-3E58912823E9";

    private String WSURL="https://signer-emisores.feel.com.gt/sign_solicitud_firmas/firma_xml";
    private String WSURLCert="https://certificador.feel.com.gt/fel/certificacion/dte/";
    private String WSURLAnul="https://certificador.feel.com.gt/fel/anulacion/dte/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        super.InitBase();

        fel=new clsFELInFile(this,this);
    }

    public void doCert(View view) {
        toastlong("Procesando factura electronica\nEspere, por favor . . .");
        buildFactXML();
    }

    public void doAnul(View view) {
        buildAnulXML();
        sendJSONAnul();
    }

    @Override
    public void felCallBack() throws Exception {
         if (!fel.errorflag) {
            toast("Factura electronica certificada.");
        } else {
            toast("Ocurrio error en FEL :\n\n"+ fel.error);
        }
    }

    private void buildFactXML() {
        try {
            fel.iniciar(2005141102);
            fel.emisor("GEN","1","","1000000000K","DEMO");
            fel.emisorDireccion("Direccion","GUATEMALA","GUATEMALA","GT");
            fel.receptor("CF","Consumidor Final","Ciudad");

            fel.detalle("Producto 1",1,"UNI",10,10,0);
            //fact.detalle("Producto 2",2,"UNI",15,30,0);

            fel.completar("ZR37-46");

            fel.certificacion();
        } catch (Exception e) {
            msgbox(new Object(){}.getClass().getEnclosingMethod().getName()+" . "+e.getMessage());
        }
    }

    private void buildAnulXML() {
        try {
             fel.anulfact("A16B83DB-5FA0-4C31-8F49-BC0465BD05DE","1000000000K","76365204",2005151636,2005151636);
            //fel.anulfact("A16B83DB-5FA0-4C31-8F49-BC0465BD05DE","1000000000K","76365204",2005151530,2005151530);
            //fel.anulfact("DB93AF6F-87A6-4BA6-A22A-01873CC93776","1000000000K","CF",1906100858,1906100858);
        } catch (Exception e) {
            msgbox(new Object(){}.getClass().getEnclosingMethod().getName()+" . "+e.getMessage());
        }
    }

    //region Anulacion

    public void sendJSONAnul() {
        try {
            s64= fel.toBase64();

            jsona = new JSONObject();

            //jsona.put("nit_emisor","1000000000K");
            //jsona.put("correo_copia","");

            //jsona.put("nit_emisor","47941162");

            jsona.put("nit_emisor","76365204");
            jsona.put("correo_copia","demo@demo.com.gt");
            jsona.put("xml_dte",s64);

            executeWSAnul();
        } catch (Exception e) {
            msgbox(e.getMessage());
        }
    }

    public void executeWSAnul() {
        jsanul = jsona.toString();
        errorflag=false;error="";

        AsyncCallWSAnul wstask = new AsyncCallWSAnul();
        wstask.execute();
    }

    public void wsExecuteA(){
        URL url;
        HttpURLConnection connection = null;
        JSONObject jObj = null;

        try {
            //Create connection
            url = new URL(WSURLAnul);
            connection = (HttpURLConnection)url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
            connection.setRequestProperty("Content-Length",""+Integer.toString(jsanul.getBytes().length));
            connection.setRequestProperty("usuario",fel_alias);
            connection.setRequestProperty("llave", fel_llavews);
            connection.setRequestProperty("identificador",fel_ident);

            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
            wr.writeBytes (jsanul);
            wr.flush ();
            wr.close ();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder sb = new StringBuilder();

            while((line = rd.readLine()) != null) {
                sb.append(line + "\n");
            }
            rd.close();

            String jstr=sb.toString();
            jObj = new JSONObject(jstr);

            error= jObj.getString("descripcion");
            if (jObj.getBoolean("resultado")) {
                errorflag=false;
            } else {
                errorflag=true;
            }

        } catch (Exception e) {
            error=e.getMessage();errorflag=true;
        } finally {
            if (connection!=null) connection.disconnect();
        }
    }

    public void wsFinishedA() {
        try  {
            if (!errorflag) {
                msgbox("Anulación correcta");
            } else {
                msgbox("Ocurrio error\n\n"+error);
            }
        } catch (Exception e)  {
        }
    }

    private class AsyncCallWSAnul extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params)  {
            try  {
                wsExecuteA();
            } catch (Exception e) { }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            try {
                wsFinishedA();
            } catch (Exception e)  {}
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}

    }

    //endregion

}
