We need some math to help us go between user intuitions like "forward" and "turn right" and our left wheel / right wheel motors.  That is, when the user thinks "full speed ahead (no turning)!", we want that to translate into "left wheel full, right wheel full", and when the user wants to turn right as tightly as possible, that translates into "left wheel full forward, right wheel full reverse".

The coordinate space of user control that seems to make the most sense for a user tilting a phone, touching a screen, or tilting and turning a space navigator, is what I'll call the FT axes: forwardness and turningness.  And we like to use values in the range -100 to 100.  So 100,0 is full forward, no turning.  100,100 is forward but also right.  -100,0 is full backward.

The coordinate space to control our wheels are the LR axes, left wheel and right wheel.  So 100,100 is full forward, 100,-100 is spinning right, and -100,-100 is full backward.

<img src='http://lh4.ggpht.com/_oXIW_jM0QDA/S85TIr2HCPI/AAAAAAAAQsU/-vsbcPS2i3c/s576/robot-axes.png'>

In the above image, you can see that the two sets of coordinates are at 45 degrees to each other: moving forward at full speed (F=100,T=0) becomes left and right at full (L=100,R=100), and so forth.<br>
<br>
You'll also notice that if you want "full forward, no rotation", (the top edge of your screen if you were going to use the touch screen as your controller) to be L=100 and R=100, then if you touch the upper right of your screen "full forward AND full rotation right", you're asking the left motor to go faster than its top speed.<br>
<br>
So going between coordinate systems means rotating + or - 45 degrees, and then scaling back (without changing the direction of your vector) until it's in the range of what the motors can do.  To rotate, we can use a rotation matrix:<br>
<br>
<pre><code>[L, R] = [f, t] * | cos(-45), -sin(-45) |<br>
                  | sin(-45), cos(-45)  |<br>
<br>
So, L = f*cos(-45) + t*sin(-45)  = f*0.707  + t*-0.707 = 0.707 * (f-t)<br>
    R = -f*sin(-45) + t*cos(-45) = -f*-0.707 + t*0.707 = 0.707 * (f+t)<br>
</code></pre>

Now, if our [f,t] values are in -100..100, then [100,0] will rotate to [70,70], so the robot will only go forward at 70% speed.  So we need to scale by 1/0.707.  So now we have:<br>
<br>
<pre><code>L = 1/0.707 * 0.707 * (f-t) = f-t<br>
R = 1/0.707 * 0.707 * (f+t) = f+t<br>
</code></pre>

Nice and simple!  So now the red control diamond is just big enough that "full forward" is L=100 and R=100.  What do we do when they ask for "full forward and full turn"?  We scale back the vector, preserving its direction, so that your forwardness and turniness don't change, but so that one of the motors is working as hard as it can.  That means we have to scale back enough that whichever one is bigger ends up as 100.<br>
<br>
<pre><code>// only scale if we're out of bounds<br>
if (abs(L) &gt; 100 || abs(R) &gt; 100) {<br>
  scale = 1.0;<br>
  // if left is bigger, that's the one to scale by<br>
  if (abs(L) &gt; abs(R)) {<br>
    scale = 100 / L;<br>
  } else {<br>
    scale = 100 / R;<br>
  }<br>
  L = scale * L;<br>
  R = scale * R;<br>
}<br>
</code></pre>

I think that does it.  So now if you draw an imaginary diamond in your touchscreen, staying inside it will affect the motor speeds, and going outside will keep moving and turning the amount you expect, but one of the motors will be maxed out, so it won't go as fast as you want.