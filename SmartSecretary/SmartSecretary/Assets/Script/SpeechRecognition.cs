using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
//using UnityEngine.Windows.Speech;
//using System.Linq;
using DigitalRuby.RainMaker;

public class SpeechRecognition : MonoBehaviour {
	public TextMesh textmesh;
	public GameObject blueboy;
    public GameObject rain;
    public GameObject cam;
    public Light lt;
    public Material rainCloudMat, cloudDefault, cloudMat1, cloudMat2, cloudMat3, cloudMat4;

    void Awake() {
		PluginManager.Getinstance();

        blueboy = GameObject.FindGameObjectWithTag("Blueboy");
        rain = GameObject.FindGameObjectWithTag("Rain");
        cam = GameObject.FindGameObjectWithTag("MainCamera");
        //textmesh = GetComponent<TextMesh>();
        lt = GetComponent<Light>();

    }
    
    void Start()
    {
        PluginManager.Getinstance().setcallbackSTTresult(new delegateSTTresult(STTresult));
        PluginManager.Getinstance().setcallbackMSG(new delegateMSG(androidMSG));

        Call_STTstart();
        print("speech recognizer : start");
       
        rain.SetActive(false);
    }
    void Update()
    {
        if (Input.GetKey(KeyCode.Escape))

        {
            Application.Quit();
        }
    }

	void STTresult(string result) {

        if (result.StartsWith("weather/", StringComparison.InvariantCultureIgnoreCase)){
            splitWeather(result);
        }
        else
        {
            makelog(result);
        }

    }
	void androidMSG(string msg) {
		//makelog("LOG : " + msg);
	}
	void makelog(string logmsg){
		print ("MAKELOG: "+logmsg);
        textmesh.text = logmsg;
    }

	public void Call_STTstart()
    {
        print ("speech recognizer : call_sttstart");
		PluginManager.Getinstance().call_androidSTT("en-US");
        //PluginManager.Getinstance().call_androidSTT("ko");

        blueboy.GetComponent<TestController>().AnimateInitialize();
    }

    void splitWeather(string w)
    {
        string[] info = w.Split('/');
        //1: day
        //2: weather name <-
        //3: cloud sort
        //4: cloud value <-
        //5: cloud per
        //6: wind name
        //7: wind speed
        //8: max temperature <-
        //9: min temperature
        //10: humidity

        if (info[2].Contains(" rain"))
        {
            makelog("It's raining");
            Rain(info[2]);
        }
        else if (Int32.Parse(info[4]) >= 20)
        {
            makelog("It's cloudy");
            Cloud(Int32.Parse(info[4]));
        }
        else
        {
            makelog("It's sunny");
            Sunny((Int32.Parse(info[8]) + Int32.Parse(info[9])) / 2);
        }
        
    }


    public void Rain(string r)
    {
        cam.GetComponent<Skybox>().material = rainCloudMat;
        rain.SetActive(true);
        lt.intensity = 0.4f;
        blueboy.GetComponent<TestController>().AnimationUpdate("raining");

        if(r.Equals("light rain"))
        {
            rain.GetComponent<RainScript>().RainIntensity = 0.3f;
            //
        }
        else if (r.Equals("moderate rain"))
        {
            //>= 0.33f
            rain.GetComponent<RainScript>().RainIntensity = 0.6f;
        }
        else
        {
            //>= 0.67f
            rain.GetComponent<RainScript>().RainIntensity = 1.0f;
        }

        //float phi = Time.time / duration * 2 * Mathf.PI;
        //float amplitude = Mathf.Cos(phi) * 0.5F + 0.5F;
        //lt.intensity = amplitude;
    }

    public void Cloud(float amount)
    {
        if (amount < 40)
        {
            cam.GetComponent<Skybox>().material = cloudMat1;
            lt.intensity = 0.8f;
        }
        else if(40 <= amount && amount < 60)
        {
            cam.GetComponent<Skybox>().material = cloudMat2;
            lt.intensity = 0.6f;

        }
        else if (60 <= amount && amount < 80)
        {
            cam.GetComponent<Skybox>().material = cloudMat3;
            lt.intensity = 0.4f;

        }
        else if(80<=amount && amount<=100)
        {
            cam.GetComponent<Skybox>().material = cloudMat4;
            lt.intensity = 0.2f;
        }
        else
        {
            cam.GetComponent<Skybox>().material = cloudDefault;
            lt.intensity = 1.0f;
        }
    }

    public void Sunny(float tempAvg)
    {
        if(tempAvg >= 20 && tempAvg < 30)
        {
            lt.intensity = 1.6f;
            blueboy.GetComponent<TestController>().AnimationUpdate("hot");
        }
        else if(tempAvg >=30 && tempAvg <40)
        {
            lt.intensity = 2.3f;
            blueboy.GetComponent<TestController>().AnimationUpdate("hot");
        }
        cam.GetComponent<Skybox>().material = cloudDefault;
    }
}