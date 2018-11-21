## Getting Started:

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

1. clone the project via git command or download the source code

```javaScript
git clone https://github.com/chabokpush/chabok-starter-gonative.git
```

2. Open Terminal app and install the ChabokPush cocoapoad in iOS path:

```ruby
pod install
```

3. open project in your idea and sync it, then build the app to your device or simulator.

4. You can login with [Test environment](https://sandbox.push.adpdigital.com/login) to see the starter app info in the panel. 

Please use the following account: 

> **Username** : `starter` 
>
> **Password** : `starter`

for more information about this app please visit our [website](http://chabokpush.com) and refer to chabok [documents](http://doc.chabokpush.com) section.

## Usage

The iOS GoNative has conflict with Reachability class. Update **Reachability.h** and **Reachability.m** file in your project to [Reachability.h](https://github.com/chabokpush/chabok-starter-gonative/blob/5f1da9f96cf50db57a1a530e4e5d9548ae9f764a/ios/LeanIOS/ReachabilityGoNative.h#L49-L53) and [Reachability.m](https://github.com/chabokpush/chabok-starter-gonative/blob/eb06c7eafd18a36937981b4efb5cd3bfdab1a0d5/ios/LeanIOS/ReachabilityGoNative.m#L49-L57) file in starter sample project.
