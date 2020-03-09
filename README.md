# Factorial_Launcher
Windows Phone Inspired Launcher, written in Kotlin

Currently the project is in a really early stage, and a lot of basic features are missing. 

A short demo: 
https://www.youtube.com/watch?v=TNLCoU4hGYY

Note**
I may overhaul the home screen logic entirely, as currently Recyclerview cannot properly support the features that I want to achieve (widgets, updating tiles with animation, gallery tiles, etc), not without a lot of effort. My plan is to replace the Recyclerview with a Linearlayout inside a Scrollview, and the tiles will be directly added to the Linearlayout as individual views. 
I'll stick to Recyclerview for now and try to implement the features, but if the results are unacceptable, I will make the changes. 


Todo:

1. properly design live tiles
2. Enable search
3. Enable live tiles hosting Android widgets
4. Settings
5. Launch app animation
6. Optimize for speed
7. App list guesture
8. Homescreen gesture
