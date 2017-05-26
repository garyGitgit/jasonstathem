using System.Collections;
using System.Collections.Generic;
using UnityEngine;
//using UnityEngine.Windows.Speech;
//using System.Linq;

public class SpeechRecognition : MonoBehaviour {
	public TextMesh textmesh;
	//add
	public GameObject blueboy;
    Animator animator;

    void Awake() {
		PluginManager.Getinstance();
		//add
		blueboy = GameObject.FindGameObjectWithTag("Blueboy");
        animator = GetComponent<Animator>();

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
        AnimationUpdate();
    }

	void STTresult(string result) {
		makelog(result);
		//tc.AnimationUpdate ();
		blueboy.GetComponent<TestController>().AnimationUpdate(result);
	}
	void androidMSG(string msg) {
		//makelog("LOG : " + msg);
	}
	void makelog(string logmsg){
		print (logmsg);
		textmesh.text = logmsg;

	}

	public void Call_STTstart() {
		print ("speech recognizer : call_sttstart");
		PluginManager.Getinstance().call_androidSTT("en-US");
	}

    public void AnimationUpdate()
    {

        //print ("");
        //Debug.Log ("STT result를 통해  update!!");
        print("STT result를 통해  update!!");
        //call every frame
        if (Input.inputString.Equals("c"))
        {
            animator.SetBool("isCalling", true);
        }
        else
        {
            animator.SetBool("isCalling", false);
        }
        if (Input.inputString.Equals("m"))
        {
            animator.SetBool("isMessage", true);
        }
        else
        {
            animator.SetBool("isMessage", false);
        }

        if (Input.inputString.Equals("r"))
        {
            animator.SetBool("isRain", true);
        }
        else
        {
            animator.SetBool("isRain", false);
        }

        if (Input.inputString.Equals("h"))
        {
            animator.SetBool("isHot", true);
        }
        else
        {
            animator.SetBool("isHot", false);
        }

        if (Input.inputString.Equals("w"))
        {
            animator.SetBool("isWeather", true);
        }
        else
        {
            animator.SetBool("isWeather", false);
        }
    }

    public void AnimationUpdate(string message)
    {

        //print ("");
        //Debug.Log ("STT result를 통해  update!!");
        print("STT result를 통해  update!! - string");
        //call every frame
        if (message.Equals("call"))
        {
            animator.SetBool("isCalling", true);
        }
        else
        {
            animator.SetBool("isCalling", false);
        }
        if (message.Equals("message"))
        {
            animator.SetBool("isMessage", true);
        }
        else
        {
            animator.SetBool("isMessage", false);
        }

        if (message.Equals("weather"))
        {
            animator.SetBool("isWeather", true);
        }
        else
        {
            animator.SetBool("isWeather", false);
        }
    }

}