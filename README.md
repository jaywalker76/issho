# Issho
A collection of standalone music tools.

# Hachi
A multi-function midi sequencer that runs on a Novation Launchpad Pro and a headless computer 
like a Raspberry Pi. Hachi is written in Java, and so can run on a Pi (tested), 
a Mac (tested), and many other platforms (untested), with or without keyboard and monitor.

Hachi (= "eight" in Japanese) can run up to 8 "modules" at a time. Available modules 
include several midi sequencers, a paint/animation toy, and a sequencer-controller that 
lets you coordinate your sequencers. Hachi is a generalized version 
of [this simpler sequencer](https://github.com/perkowitz/sequence) 
that I wrote for the original Launchpad. 
That basic sequencer runs as one module in Hachi.

# Documentation

- [Getting Started](doc/getting-started.md): how to set up your computer and MIDI devices to run Issho applications.
- [Hachi](doc/hachi/hachi.md): how to use Hachi, and how to configure and use various modules.
- [Hachi Development](doc/hachi/development.md): details about Hachi's code and how to write your own modules. 

# Issues

Tracking bugs, needed improvements, and all-new features using [issues](https://github.com/perkowitz/issho/issues) 
here in Github, but also via [HuBoard](https://huboard.com/perkowitz/issho#/milestones). 

# Release notes

## 2017-09-02 v1.0.2
- Session copy/clear from settings screen for Beatbox & Para
- Follow 24ppqn MIDI clock, enable swing by MIDI pulse
- Allow multiple devices to control Hachi simultaneously
- Beatbox replaces Rhythm
- Minibeat module
- Memory management app

## 2017-01-29: v1.0.1

- Add configurable keyboard device
- Added Chords, ChordReceiver, ChordModule to track chords from keyboard device and apply chords to outgoing MIDI notes
- Updated MonoModule and StepModule to extend ChordModule so they follow chords
- Added chord options to keyboard configuration
- Updated doc to cover chords and keyboard configuration
- Added tests for Chords

# Hachi

Here is our friend, the other Hachi: 

![The Other Hachi](doc/hachi/hachi-face.jpg)