<div align="center">
  <img src="https://www.tensorflow.org/images/tf_logo_transp.png"><br><br>
</div>

-----------------


| **`Documentation`** |
|-----------------|
| [![Documentation](https://img.shields.io/badge/api-reference-blue.svg)](https://www.tensorflow.org/api_docs/) |


# TensorFlow Android Camera Demo

This folder contains an example application utilizing TensorFlow for Android
devices.

## Description

The demos in this folder are designed to give straightforward samples of using
TensorFlow in mobile applications.

Inference is done using the [TensorFlow Android Inference
Interface](../../../tensorflow/contrib/android), which may be built separately
if you want a standalone library to drop into your existing application. Object
tracking and efficient YUV -> RGB conversion are handled by
`libtensorflow_demo.so`.

A device running Android 5.0 (API 21) or higher is required to run the demo due
to the use of the camera2 API, although the native libraries themselves can run
on API >= 14 devices.

## Current samples:

1. [Detection des boutons | Tensorflow : enabled](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/ClassifierActivity.java):
        Demonstrates an Faster-RCNN model trained using the
        [Tensorflow Object Detection API](https://gitlab-lyon.sqli.com/ebmourabit/visual-recognition-server-control-ionic-back) to classify camera uploded or taken images, with displaying the recognition results.

2. [TF Classify : disabled](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/ClassifierActivity.java):
        Uses the [Google Inception](https://arxiv.org/abs/1409.4842)
        model to classify camera frames in real-time, displaying the top results
        in an overlay on the camera image.
3. [TF Detect : disabled](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/DetectorActivity.java):
        Demonstrates an SSD-Mobilenet model trained using the
        [Tensorflow Object Detection API](https://github.com/tensorflow/models/tree/master/research/object_detection/)
        introduced in [Speed/accuracy trade-offs for modern convolutional object detectors](https://arxiv.org/abs/1611.10012) to
        localize and track objects (from 80 categories) in the camera preview
        in real-time.
4. [TF Stylize : disabled](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/StylizeActivity.java):
        Uses a model based on [A Learned Representation For Artistic Style](https://arxiv.org/abs/1610.07629) to restyle the camera preview
        image to that of a number of different artists.
5.  [TF Speech : disabled](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/android/src/org/tensorflow/demo/SpeechActivity.java):
    Runs a simple speech recognition model built by the [audio trainin tutorial](https://www.tensorflow.org/versions/master/tutorials/audio_recognition). Listens
    for a small set of words, and highlights them in the UI when they are
    recognized.
        
<img src="sample_images/classify1.jpg" width="30%"><img src="sample_images/stylize1.jpg" width="30%"><img src="sample_images/detect1.jpg" width="30%">

## Prebuilt Components:

If you just want the fastest path to trying the demo, you may download the
nightly build
[here](https://ci.tensorflow.org/view/Nightly/job/nightly-android/). Expand the
"View" and then the "out" folders under "Last Successful Artifacts" to find
tensorflow_demo.apk.

Also available are precompiled native libraries, and a jcenter package that you
may simply drop into your own applications. See
[tensorflow/contrib/android/README.md](../../../tensorflow/contrib/android/README.md)
for more details.

## Running the Demo

### 1 - Explanations

Few important pointers that we should know:

    The core of the TensorFlow is written in c++.
    In order to build for Android, we have to use JNI(Java Native Interface) to call the c++ functions like loadModel, getPredictions, etc.
    We will have a .so(shared object) file which is a c++ compiled file and a jar file which will consist of JAVA API that will be calling the native c++. And then, we will be calling the JAVA API to get things done easily.
    So, we need the jar(Java API) and a .so(c++ compiled) file.
    We must have the pre-trained model file and a label file for the classification

### 2 - Procedures:

    First clone the tensorflow android repo from this link and store in your project folder:

    git clone –recurse-submodules https://github.com/tensorflow/tensorflow.git

    Get installed Android Studio

    Download the latest version of the NDK



Once the app is installed it can be started via the "TF Classify" icon, which have the orange TensorFlow logo as
their icon.


