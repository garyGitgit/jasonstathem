using System.Collections;
using System.Collections.Generic;
using UnityEngine;


public delegate void delegateSTTresult(string result);
public delegate void delegateMSG(string msg);


public class PluginManager : MonoBehaviour {
	

	private static PluginManager instance;
	private static GameObject content;
	public static PluginManager Getinstance() {
		if (instance == null) {
			content = new GameObject();
			content.name = "pluginUnity"; 
			instance = content.AddComponent(typeof(PluginManager))as PluginManager;
			print ("plugin maanger : get instance");
		}
		return instance;
	}

	private AndroidJavaObject AJO = null;
	private AndroidJavaClass AJC = null;

	delegateSTTresult _STTresult;
	delegateMSG _MSG;

	void Awake() {
//		#if UNITY_ANDROID && !UNITY_EDITOR
		print("inAwake()");
//		AJC = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
//		AJO = AJC.GetStatic<AndroidJavaObject>("currentActivity");
//		#endif
//		print ("plugin maanger : Awake()");
		#if UNITY_ANDROID && !UNITY_EDITOR
		print("inAwake()");
		AJC = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		AJO = AJC.GetStatic<AndroidJavaObject>("currentActivity");
		#endif
		print ("plugin maanger : Start()");
	}
	void Start(){
		
	}

	public void call_androidSTT(string lang) {
		print ("plugin maanger : start speech reco");
		print (lang);
		AJO.Call("StartSpeechReco", lang);

	}
	public void msgUnity(string msg) {
		if (_MSG != null) {
			_MSG(msg);
		}
	}

	public void sttUnity(string result) {
		if (_STTresult != null) {
			_STTresult(result);
		}
	}
	public void setcallbackSTTresult(delegateSTTresult callback) { this._STTresult = callback; }
	public void setcallbackMSG(delegateMSG callback) { this._MSG = callback; }

}
