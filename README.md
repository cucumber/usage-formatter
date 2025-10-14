# Usage Formatter
⚠️ This is an internal package; you don't need to install it in order to use the Usage Formatter.

[![Maven Central](https://img.shields.io/maven-central/v/io.cucumber/cucumber-json-formatter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:io.cucumber%20AND%20a:usage-formatter)

Writes usage statistics for step definitions

## Features

Can render a plain text report showing which step definitions are used by which
steps and some statistics for long each took. The error from the mean is the 95%
confidence interval assuming a normal distribution.

```
Expression/Text              Duration   Mean ±  Error Location                                               
an order for {string}          0.009s 0.001s ± 0.000s samples/multiple-features/multiple-features.ts:3       
  an order for "eggs"          0.001s                 samples/multiple-features/multiple-features-1.feature:3
  an order for "milk"          0.001s                 samples/multiple-features/multiple-features-1.feature:6
  an order for "bread"         0.001s                 samples/multiple-features/multiple-features-1.feature:9
  an order for "batteries"     0.001s                 samples/multiple-features/multiple-features-2.feature:3
  an order for "light bulbs"   0.001s                 samples/multiple-features/multiple-features-2.feature:6
  4 more                                                                                                     
```

The output can also be rendered as a json report.

```json
{
  "stepDefinitions": [
    {
      "sourceReference": {
        "uri": "samples/multiple-features/multiple-features.ts",
        "location": {
          "line": 3
        }
      },
      "duration": {
        "sum": {
          "seconds": 0,
          "nanos": 9000000
        },
        "mean": {
          "seconds": 0,
          "nanos": 1000000
        },
        "moe95": {
          "seconds": 0,
          "nanos": 0
        }
      },
      "expression": {
        "source": "an order for {string}",
        "type": "CUCUMBER_EXPRESSION"
      },
      "matches": [
        {
          "text": "an order for \"eggs\"",
          "duration": {
            "seconds": 0,
            "nanos": 1000000
          },
          "uri": "samples/multiple-features/multiple-features-1.feature",
          "location": {
            "line": 3,
            "column": 3
          }
        },
...
```


## Contributing

Each language implementation validates itself against the examples in the
`testdata` folder. See the [testdata/README.md](testdata/README.md) for more
information.
