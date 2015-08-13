package com.beseye.stringparser;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class ParserActivity extends Activity {
	private EditText mEtImportFilePath , mEtExportFilePath;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parser);
        
        Button btnExport = (Button)findViewById(R.id.btn_export);
        btnExport.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				try {
					ToolExport.run("/storage/emulated/0/Download", mEtExportFilePath.getText().toString());
					Toast.makeText(ParserActivity.this, "export string to "+mEtExportFilePath.getText().toString()+" ok!!", Toast.LENGTH_SHORT).show();
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(ParserActivity.this, "export string error\n"+e.toString(), Toast.LENGTH_SHORT).show();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}
			}});
        
        mEtExportFilePath = (EditText)findViewById(R.id.et_export_file);
       
        Button btnImport = (Button)findViewById(R.id.btn_import);
        btnImport.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				try {
					;
					ToolImport.run(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/"+mEtImportFilePath.getText().toString());
					Toast.makeText(ParserActivity.this, "import string from "+mEtImportFilePath.getText().toString()+" ok!!", Toast.LENGTH_SHORT).show();
				} catch (TransformerException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(ParserActivity.this, "import string error\n"+e.toString(), Toast.LENGTH_SHORT).show();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}
			}});
        
        mEtImportFilePath = (EditText)findViewById(R.id.et_import_file);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.parser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
