# Download

```
implementation 'com.github.lienilibrary:EasyHttp:1.0.0'
```

# Usage

### step 1: initialize in application

```
EasyHttp.init(new EasyBuilder(this,BuildConfig.SERVER_HOST));
```

### step 2: use

```
EasyHttp.create(BaseService.class)
```