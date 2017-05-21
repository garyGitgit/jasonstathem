using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TestController : MonoBehaviour
{

    public float speed = 2f;
    public float rotateSpeed = 100f;
    public float jumpPower = 5f;
    public Transform tr;

    float horizontal, vertical;

    Rigidbody rb;
    Animator animator;

    public bool isHear = false;

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

        AnimationUpdate();
    }

    void FixedUpdate()
    {
    }

    void AnimationUpdate()
    {
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

        if (Input.inputString.Equals("r"))
        {
            animator.SetBool("isHear", true);
        }
        else
        {
            animator.SetBool("isHear", false);
        }

    }
    
}


