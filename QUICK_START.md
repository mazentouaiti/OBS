# ⚡ Quick Start: Auto Features

## 🎯 What's New?

Two automatic features powered by sensors:

### 1️⃣ **Auto Focus on Proximity** (NEW!)
When proximity sensor detects object near → Camera auto-focuses
- Enable in: **Sensors Settings → Toggle "Proximity"**
- Works in: Camera activity
- Range: Typically 0-5cm

### 2️⃣ **Auto Brightness Control** (Added Recently)
When light sensor detects ambient light → Screen brightness adjusts
- Enable in: **Sensors Settings → Toggle "Light"**
- Works in: Both Camera and Settings activities
- Range: 0-50,000+ lux

---

## 🚀 Quick Start

### **To Use Auto Focus:**
1. Open Camera app
2. Tap menu → **Sensors Settings**
3. Toggle **"Proximity"** ON
4. Watch for message: `⭐ Auto-focus enabled in Camera`
5. Bring your face near phone
6. Camera automatically focuses! 📸

### **To Use Auto Brightness:**
1. Open Camera app
2. Tap menu → **Sensors Settings**
3. Toggle **"Light"** ON
4. Screen brightness auto-adjusts to room lighting
5. Works in dim rooms and bright sunlight ☀️

---

## 📱 Testing in Sensors Activity

**Test Auto Focus:**
- Settings app → **Sensors Settings** tab
- Toggle **"Proximity"** ON
- Hover hand over phone back
- Watch distance reading change
- Camera auto-focuses along with it

**Test Auto Brightness:**
- Settings app → **Sensors Settings** tab
- Toggle **"Light"** ON
- Move phone to different lighting
- Screen brightness changes in real-time

---

## 🔍 Status Indicators

### Sensors Activity Display:
```
✓ Proximity: Active
  Distance: 3.2 cm
  State: NEAR
  ⭐ Auto-focus enabled in Camera

✓ Light Sensor: Active
  Light: 450.0 lux
  Category: Normal
  Brightness: 0.68
```

### Camera Activity Logs:
```
Auto Focus: Object detected at 3.2 cm - focusing...
Auto Focus: Object far (8.5 cm) - continuous focus active
Auto Brightness: 450.0 lux -> 0.68 brightness
```

---

## ⚙️ How It Works (Simple)

### **Proximity → Auto Focus**
```
Proximity Sensor detects object
           ↓
Is it close? YES → Trigger Camera Focus
           ↓ NO
    Keep continuous focus
```

### **Light → Auto Brightness**
```
Light Sensor measures lux
           ↓
Dark? → Dim screen
Bright? → Brighten screen
Normal? → Medium brightness
```

---

## 🛠️ Troubleshooting

### Auto Focus Not Working?
- [ ] Is proximity sensor toggled ON?
- [ ] Does phone have proximity sensor? (most do)
- [ ] Are you in camera activity?
- [ ] Check: Camera permission granted?

### Brightness Not Changing?
- [ ] Is light sensor toggled ON?
- [ ] Does phone have light sensor? (most do)
- [ ] Check system brightness auto setting (might override)
- [ ] Try moving to much darker/brighter location

---

## 📊 Sensor Ranges

### Proximity
- Detection: 0-5cm (near) vs 5cm+ (far)
- Accuracy: Binary or distance (device dependent)
- Response: ~100-200ms

### Light
- Very Dark: < 10 lux (screen at 0.2 brightness)
- Dark: 10-50 lux (screen at 0.2-0.5)
- Normal: 50-500 lux (screen at 0.5-0.8)
- Bright: 500-10,000 lux (screen at 0.8-1.0)
- Very Bright: 10,000+ lux (screen at 1.0)

---

## 💡 Tips & Tricks

✅ **Both features work simultaneously** - Use them together!
✅ **Enable in Settings, use in Camera** - Settings feed the data
✅ **Fully automatic** - No manual adjustments needed
✅ **Can toggle on/off anytime** - Changes take effect immediately
✅ **Check logcat for debug info** - See what's happening under the hood

---

## 🎥 Use Cases

### Perfect For:
- 📸 Hands-free vlogging (focus on face, brightness auto-adjusts)
- 🌙 Low-light recording (auto-dims screen, bright display not needed)
- ☀️ Outdoor filming (screen stays visible in sunlight)
- 🎭 Face recording (auto-focus when you come close)
- 📹 Indoor/outdoor transitions (smooth lighting adjustment)

---

## 📞 Quick Reference

| Task | Where | How |
|------|-------|-----|
| Enable Auto Focus | Settings | Sensors → Toggle Proximity |
| Disable Auto Focus | Settings | Sensors → Toggle Proximity |
| Enable Auto Brightness | Settings | Sensors → Toggle Light |
| Disable Auto Brightness | Settings | Sensors → Toggle Light |
| See Focus Status | Camera Activity | Watch proximity overlay |
| See Brightness Status | Sensors Activity | Watch light readings |
| Test Features | Sensors Activity | Toggle and watch values |
| Debug | Terminal | `adb logcat \| grep "Auto"` |

---

## 🎯 Next Steps

1. ✅ Test in Sensors Activity first
2. ✅ Then test in Camera Activity
3. ✅ Try different lighting conditions
4. ✅ Try different distances for proximity
5. ✅ Use together for best experience
6. ✅ Check logcat if anything feels off

---

**Ready to go!** 🚀 Both features are fully integrated and production-ready.


