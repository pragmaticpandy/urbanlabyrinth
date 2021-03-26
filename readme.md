## About

I have achieved peak covid.

This past weekend I was feeling trapped indoors and unable to escape. The intersection of:

* me being on-call for work, requiring me to be always just a few minutes from my work laptop,
* my work laptop having a terrible, several years old
  [butterfly keyboard](https://www.macrumors.com/guide/butterfly-keyboard-vs-scissor-keyboard/),
  with multiple non-deterministic keys, making the external keyboard at my home "office" desk
  mandatory for actually getting things done,
* and, oh yeah, a fucking pandemic,

yeah, had me feeling *rather* trapped.

My solution to this problem? A program that given a grid and starting location, generates
labyrinth-like round trip walking paths that maximize the number of unique sidewalk segments in the
path. I.e., you can take just the generated instructions and your pager, and go on a long,
meditative, disconnected walk while the entire time being within three blocks from home.

### Example

Specify the street grid, the starting (and ending) corner, and any sidewalk segments to exclude:

```
// west to east
val verticalStreets = listOf(Street("15th"), Street("16th"), Street("17th"), Street("18th"),
Street("19th"))

// north to south
val horizontalStreets = listOf(Street("John"), Street("Denny"), Street("Howell"), Street("Olive"),
Street("Pine"))

val startingCorner = Corner(Street("17th"), Street("Howell"))

// excluded segments ommitted for this readme example, but they are colored in the image below
```

Then run it, and it will output many paths, such as this high-scoring one:

```
Start at the northeast corner of 17th and Howell and head north on 17th
At John, don't cross and turn right
At 18th, don't cross and turn right
At Howell, don't cross and turn right
At 16th, don't cross and turn right
At Denny, don't cross and turn right
At 19th, don't cross and turn right
At Howell, don't cross and turn right
At 18th, don't cross and turn right
At John, don't cross and turn right
At 19th, don't cross and turn right
At Denny, don't cross and turn right
At 16th, don't cross and turn right
At John, don't cross and turn right
At 17th, don't cross and turn right
At Olive, don't cross and turn right
At 15th, don't cross and turn right
At Howell, don't cross and turn right
At 16th, don't cross and turn right
At Pine, don't cross and turn right
At 15th, don't cross and turn right
At Olive, don't cross and turn right
At 18th, cross Olive and turn left
At Howell, don't cross and turn left
At 17th, don't cross and turn left
At Olive, don't cross and turn left
At 18th, cross 18th and turn left
At Howell, don't cross and turn right
At 19th, cross both streets and turn left
At John, cross both streets and turn left
At 16th, cross both streets and turn left
At Denny, don't cross and turn right
At 15th, cross Denny and turn left
At Howell, don't cross and turn left
At 16th, don't cross and turn left
At Denny, don't cross and turn left
At 15th, cross 15th and turn left
At Pine, cross 15th and turn left
At 16th, cross 16th and turn left
At Howell, don't cross and turn right
Done.
```

![](https://github.com/pragmaticpandy/urbanlabyrinth/raw/main/documentation/readme-example-map.png)

### Scoring

Right now scoring is simply:

* +10 for each unique sidewalk segment
* -9 for each reused sidewalk segment
* -1 for each street crossing, except when the cross is a single, straight cross
* -27 for each u-turn

### Limitations

The primary obvious limitation is that this assumes a normal grid of streets with sidewalks on both
sides everywhere. You can deal with minor abnormalities by excluding sidewalk segments that don't
exist, but as of now this program won't work well for a neighborhood with highly irregular streets.

## Usage/dev
1. Requirements: `gradle` and a JRE
1. Edit the editable stuff at the top of `App.kt` to your liking.
1. `gradle run` to run.
1. Results will go to `app/output`, each prepended with its score (higher is better). There will be
   lots of results since during the search each new high score just gets dumped to a file. It's this
   way so that, in the case that a long-running search is interrupted, not all is lost. Also because
   laziness.
