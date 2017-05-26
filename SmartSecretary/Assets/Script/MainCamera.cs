using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class MainCamera : MonoBehaviour {
    public GameObject player;

    public float offsetX = 0f;
    public float offsetY = 25f;
    public float offsetZ = -35f;
    public float followSpeed = 10f;

    Vector3 cameraPosition;

    private void LateUpdate()
    {
//        cameraPosition.x = player.transform.position.x + offsetX;
//        cameraPosition.y = player.transform.position.y + offsetY;
//        cameraPosition.z = player.transform.position.z + offsetZ;
//
//        transform.position = Vector3.Lerp(transform.position, cameraPosition, followSpeed * Time.deltaTime);
    }
}
