Debug
=====
Debug is a 3D augmented-reality game which encourages people do exercise in a fun way. You will get a lot of sweat while using your feet to "burn" and "squash" bugs on the screen. Relax! They are not real, but almost! Rendered in 3D models, they are even smart enough to dodge your shoes and bounce right after hitting edges of floors. 

We have minimized the requirements of playing the game. Things you need to make sure:

* A good light condition while playing

* Sticky notes/markers of a high contrast color with the floor.


We are keen on having your feedback. Feel free to contact:  czxttkl@gmail.com & shangma.1st@gmail.com 

Happy debugging!


Techniques Used
--------------------------------------
**Sensors**:`Accelerometer`  `Linear Accelerometer` `Orientation Sensor` 

Accelerometer is used for detecting if users hold phones horizontally. Linear Accelerometer is used for jumping detection. We tried the orientation sensor to acquire walking directions while users are playing the game. However, we abandoned using it in the current version because the Optical Flow algorithm will take replace of it. 

**OpenCV**:`Optical Flow` `Color Blob Detection` `Template Matching` `Hough Lines`

Optical Flow is implemented to detect directions and speeds that users are walking in so that bugs would move correspondingly. Currently, we use Color Blob to identify players' shoes and floors they are on. It is not the best solution, though. Before that we have also tried Template Matching and Hough Lines for the same goals, which turned out to suffer from the limited performance on mobile devices.

**OpenGL ES 2.0**:`OBJ loading` `Orthogonal Projection`
For some convenience we employ Orthogonal Projection at this moment. The bug's model comes from a free download obj file. We also need to parse it to convert it into four float arrays representing bugs' color, position, normals and textures.

**Java**:`Singleton` `Observer & Observable` `Reentrant Lock`

Screenshots
--------------------------------------
![](https://dl.dropboxusercontent.com/u/5055823/debug_pic1.PNG)   
![](https://dl.dropboxusercontent.com/u/5055823/debug_pic2.PNG) 

[![](https://dl.dropboxusercontent.com/u/5055823/debug_pic3.PNG)](https://www.youtube.com/watch?v=gx8iwlgv9A8)




