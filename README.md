# unitard

Provides a safe way to describe, traverse, edit and use untyped data in Java.

Chuck everything in an immutable, recursive data structure `Stuff`, containing untyped maps and lists.  Navigating, accessing, casting, setting, adding, removing and modifying values can be done safely without the possibility of exceptions thrown.

Results are returned in a `Hopefully<A>` data type; this is an ADT representing one of:
- The successfully retrieved and casted non-null value
- The successfully retrieved but null value
- Error: missing value (ie invalid path)
- Error: casting failed 

This is obviously applicable for manipulating JSON-shaped data, but is not a JSON library.  The maps and lists may contain arbitrary instances of JVM classes.

### Examples
Take the following JSON structure:

```
{
  "tieredResults": [
    { 
      "results": [
        {
          "channel": "buy",
          "someNullThing": null
        }
      ] 
    }
  ]
}
```

```
Stuff stuff = Stuff.buildFromMap(jsonMap);

Hopefully<String> actual = stuff.at("tieredResults", 0, "results", 0, "channel").get().as(String.class);
// ActualValue(buy)

Hopefully<String> missing = stuff.at("tieredResults", 77, "wat", "?????").get().as(String.class);
// Missing at path tieredResults[77].wat.?????

Hopefully<String> nullValue = stuff.at("tieredResults", 0, "results", 0, "someNullThing").get();
// Null at path tieredResults[0].results[0].someNullThing

Hopefully<String> wrongType = stuff.at("tieredResults", 0, "results", 0, "channel").get().as(Integer.class);
// WrongType(found=String, expected=Integer) at path tieredResults[0].results[0].channel

```
