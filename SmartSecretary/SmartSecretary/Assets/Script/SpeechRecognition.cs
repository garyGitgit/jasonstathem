using System.Collections;
using System.Collections.Generic;
using UnityEngine;
//using UnityEngine.Windows.Speech;
//using System.Linq;

public class SpeechRecognition : MonoBehaviour {
	public TextMesh textmesh;
	//add
	public GameObject blueboy;
    public GameObject rain;
    public GameObject cam;
    public Light lt;
    public Material darkCloudMat, sunnyCloudMat; 

    void Awake() {
		PluginManager.Getinstance();
        //add

        blueboy = GameObject.FindGameObjectWithTag("Blueboy");
        rain = GameObject.FindGameObjectWithTag("Rain");
        cam = GameObject.FindGameObjectWithTag("MainCamera");
        textmesh = GetComponent<TextMesh>();
        lt = GetComponent<Light>();
    }

	void Start () {
		PluginManager.Getinstance().setcallbackSTTresult(new delegateSTTresult(STTresult));
		PluginManager.Getinstance().setcallbackMSG(new delegateMSG(androidMSG));
        
        Call_STTstart ();
		print ("speech recognizer : start");
        //tc = new TestController ();

    }
    void Update()
    {
        if (Input.GetKey(KeyCode.Escape))

        {
            Application.Quit();
        }
    }

	void STTresult(string result) {
		makelog(result);
        //tc.AnimationUpdate ();

        if (result.Equals("raining"))
        {
            rain.SetActive(true);
            //D_cloud2 = Shader.Find("Assets/SimpleCloudSystem/Clouds/DarkClouds_Materials/D_Cloud2.mat");
            //Material cloudMat = new Material(D_cloud2);

            //cloudMat = Resources.Load("Assets/Resources/DarkClouds_Materials/D_Cloud2.mat", typeof(Material)) as Material;
            cam.GetComponent<Skybox>().material = darkCloudMat;

            //float phi = Time.time / duration * 2 * Mathf.PI;
            //float amplitude = Mathf.Cos(phi) * 0.5F + 0.5F;
            //lt.intensity = amplitude;
        }
        else
        {
            cam.GetComponent<Skybox>().material = sunnyCloudMat;
        }
        blueboy.GetComponent<TestController>().AnimationUpdate(result);
    }
	void androidMSG(string msg) {
		//makelog("LOG : " + msg);
	}
	void makelog(string logmsg){
		print (logmsg);
		textmesh.text = logmsg;
    }

	public void Call_STTstart()
    {
        print ("speech recognizer : call_sttstart");
		PluginManager.Getinstance().call_androidSTT("en-US");
        //PluginManager.Getinstance().call_androidSTT("ko");
        rain.SetActive(false);
    }
}