using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TestController : MonoBehaviour {

    public float speed = 2f;
    public float rotateSpeed = 100f;
    public float jumpPower = 5f;
    public Transform tr;

    float horizontal, vertical;

    Rigidbody rb;
    Animator animator;

    void Awake()
    {
        rb = GetComponent<Rigidbody>();
        tr = GetComponent<Transform>();
        animator = GetComponent<Animator>();
    }

    void Update()
    {
        horizontal = Input.GetAxis("Horizontal");
        vertical = Input.GetAxis("Vertical");

        //tr.Rotate(Vector3.up * Time.deltaTime * rotateSpeed * Input.GetAxis("Mouse X"));

        //AnimationUpdate();
    }

    void FixedUpdate()
    {
    }

    public void AnimationUpdate()
    {

		//print ("");
		//Debug.Log ("STT result를 통해  update!!");
		print ("STT result를 통해  update!!");
		//call every frame
        if(Input.inputString.Equals("c"))
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
    }

	public void AnimationUpdate(string message)
	{

		//print ("");
		//Debug.Log ("STT result를 통해  update!!");
		print ("STT result를 통해  update!! - string");
		//call every frame
		if(message.Equals("call"))
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
    

