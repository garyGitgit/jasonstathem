using System.Collections;
using System.Collections.Generic;
using UnityEngine;
public class TestController : MonoBehaviour {

    public float speed = 2f;
    public float rotateSpeed = 100f;
    public float jumpPower = 5f;
    public Transform tr;
    //public GameObject rain;

    float horizontal, vertical;

    Rigidbody rb;
    Animator animator;

    void Awake()
    {
        rb = GetComponent<Rigidbody>();
        tr = GetComponent<Transform>();
        animator = GetComponent<Animator>();
        //rain = GameObject.FindGameObjectWithTag("Rain");
        //rain.SetActive(false);
    }

    void Start()
    {
    }

    void Update()
    {
        horizontal = Input.GetAxis("Horizontal");
        vertical = Input.GetAxis("Vertical");

        //tr.Rotate(Vector3.up * Time.deltaTime * rotateSpeed * Input.GetAxis("Mouse X"));
    }

	public void AnimationUpdate(string message)
	{
        print ("Animate " + message);

        switch (message)
        {
            case "call":
                animator.SetBool("isCalling", true);
                animator.SetBool("isMessage", false);
                animator.SetBool("isWeather", false);
                animator.SetBool("isRain", false);
                break;
            case "message":
                animator.SetBool("isMessage", true);
                animator.SetBool("isCalling", false);
                animator.SetBool("isWeather", false);
                animator.SetBool("isRain", false);
                break;
            case "weather":
                animator.SetBool("isWeather", true);
                animator.SetBool("isCalling", false);
                animator.SetBool("isMessage", false);
                animator.SetBool("isRain", false);
                break;
            case "raining":
                //rain.SetActive(true);
                animator.SetBool("isRain", true);
                animator.SetBool("isWeather", false);
                animator.SetBool("isCalling", false);
                animator.SetBool("isMessage", false);
                break;
            default:
                animator.SetBool("isRain", false);
                animator.SetBool("isCalling", false);
                animator.SetBool("isMessage", false);
                animator.SetBool("isWeather", false);
                break;
        }
	}
}
    

