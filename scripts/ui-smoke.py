import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/pocket-test.xml")
    return list(ET.fromstring(adb("shell", "cat", "/sdcard/pocket-test.xml")).iter("node"))


def find(text=None, cls=None, index=0):
    for _ in range(8):
        current = nodes()
        matches = [n for n in current if (text is None or text.casefold() in (n.get("text", "").casefold(), n.get("content-desc", "").casefold())) and (cls is None or n.get("class") == cls)]
        if len(matches) > index:
            return matches[index]
        time.sleep(1)
    print([(n.get("text"), n.get("content-desc"), n.get("class")) for n in current])
    raise AssertionError("Missing control: " + str((text, cls, index)))


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def enter(node, value):
    tap(node)
    adb("shell", "input", "text", value)
    adb("shell", "input", "keyevent", "4")


enter(find(cls="android.widget.EditText", index=0), "Pocket-test-password")
enter(find(cls="android.widget.EditText", index=1), "Pocket-test-password")
tap(find("Create account"))
find("Successful")
tap(find("Continue to Pocket"))
enter(find(cls="android.widget.EditText"), "Pocket-test-password")
tap(find("Otkljucaj", cls="android.widget.Button"))
tap(find("Dodaj trosak"))
enter(find("Naziv troska"), "Market")
enter(find("Iznos KM"), "12.50")
tap(find("Sacuvaj"))
find("Potroseno: 12.50 KM")
adb("shell", "input", "keyevent", "3")
time.sleep(2)
adb("shell", "am", "start", "-W", "-n", "co.pocket.tracker/co.pocket.companion.MainActivity")
find("Lozinka")
print("Pocket UI tests passed: registration, success, unlock, expense and background lock.")
