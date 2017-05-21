using System.Collections;
using System.Collections.Generic;
using UnityEngine;
//using UnityEngine.Windows.Speech;
//using System.Linq;

public class SpeechRecognition : MonoBehaviour
{
    public TextMesh textmesh;

    Animator animator;
    public bool isCall = false;

    void Awake()
    {
        PluginManager.Getinstance();
        animator = GetComponent<Animator>();
    }
    void Start()
    {
        PluginManager.Getinstance().setcallbackSTTresult(new delegateSTTresult(STTresult));
        PluginManager.Getinstance().setcallbackMSG(new delegateMSG(androidMSG));
        print("speech recognizer : start");
    }

    void Update()
    {
        AnimationUpdate();
    }

    void STTresult(string result)
    {
        makelog(result);

        if (result=="call")
           isCall = true;
        else
           isCall = false;
    }

    void androidMSG(string msg)
    {
        makelog("LOG : " + msg);
    }

    void makelog(string logmsg)
    {
        print(logmsg);
        textmesh.text = logmsg;
    }

    public void Call_STTstart()
    {
        print("speech recognizer : call_sttstart");
        PluginManager.Getinstance().call_androidSTT("en-US");
    }

    void AnimationUpdate()
    {
        if (isCall)
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

        if (Input.inputString.Equals("w"))
        {
            animator.SetBool("isWeather", true);
        }
        else
        {
            animator.SetBool("isWeather", false);
        }

        if (Input.inputString.Equals("r"))
        {
            animator.SetBool("isRaining", true);
        }
        else
        {
            animator.SetBool("isRaining", false);
        }

        if (Input.inputString.Equals("t"))
        {
            animator.SetBool("isHot", true);
        }
        else
        {
            animator.SetBool("isHot", false);
        }

    }
}