package ru.sbt.edu_power.e2e_core.layout;

import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.layout.enums.ElementsColor;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetElement;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

class JSResources {
    static String getPageScaleScript(final double scaleFactor) {
        return "document.body.style.zoom = '" + scaleFactor * 100 + "%'";
    }
    static String getDecorationScript() {
        return "let layoutingByStyle = function (argument) {let nodes = ['div', 'a', 'p', 'button', 'span'];" +
               "nodes.forEach(node => {[...argument.getElementsByTagName(node)]" +
               ".forEach(e => {let c = window.getComputedStyle(e);" +
               "if (c['display'] !== 'none' && (c['borderRadius'] !== '0px' || c['borderWidth'] !== '0px' ||" +
               "(c['backgroundColor'] !== 'rgba(0, 0, 0, 0)' || c['backgroundImage'] !== 'none') ||" +
               "c['boxShadow'] !== 'none')) {e.classList.add('layoutDecorationElement');}});});};" +
               "layoutingByStyle(document.body);";
    }

    static String getFinishingScript() {
        return "let fs = document.createElement('script');" +
               "fs.innerText = \"function prepareData(event) {[... document.getElementsByClassName('detected-elements')]" +
               ".forEach(e => e.parentNode.removeChild(e));event.toElement.setAttribute('data-ignoredList', ids.join(','));" +
               "event.toElement.setAttribute('data-ready','true');}\";" +
               "fs.classList.add('removeAfterComplete');document.body.appendChild(fs);";
    }

    static String getSwitcherScript() {
        return "let ss = document.createElement('script');" +
               "ss.innerText = \"var ids=[''];function changeIgnoredState(event){let id = event.toElement.id;" +
               "if (ids.indexOf(id) != -1) {ids.splice(ids.indexOf(id), 1);} else {ids.push(id);}" +
               "let bgColor = ids.indexOf(id) > 0 ? 'rgba(255,0,0,.2)' : '';" +
               "event.toElement.style.backgroundColor = bgColor;}\";" +
               "ss.classList.add('removeAfterComplete');document.body.appendChild(ss);";
    }

    static String getCompleteButton() {
        return "let button = document.createElement('div'); button.style.cssText='padding: 5px 10px;text-align:center;" +
               "background:#333;color:#fff;position:fixed;top:5px;left:5px;z-index:110000;cursor:pointer';" +
               "button.innerText = 'Завершить просмотр';button.setAttribute('onclick','completeTest(event)');" +
               "button.classList.add('removeAfterComplete');document.body.appendChild(button);let cs = document.createElement('script');" +
               "cs.innerText = \"function completeTest(event){event.toElement.id='layout-testing-complete-button'}\";" +
               "button.classList.add('removeAfterComplete');document.body.appendChild(cs);";
    }

    static String getCreateButton() {
        return "let btn = document.createElement('div'); btn.style.cssText='padding: 5px 10px;" +
               "text-align:center;background:#333;color:#fff;position:fixed;top:5px;left:5px;z-index:110000;cursor:pointer';" +
               "btn.innerText = 'Создать';btn.setAttribute('onclick','prepareData(event)');" +
               "btn.id='ignored_list_generate_button';btn.classList.add('removeAfterComplete');document.body.appendChild(btn);";
    }

    static String getChangeScrollStyle() {
        final int scrollBarWidth = 0;
        return "function setScrollClass(a) {if (a.clientHeight>0 && a.scrollHeight > (a.clientHeight + 2) && 'hidden' !==" +
               "window.getComputedStyle(a)['overflow']) {a.classList.add('isScrollableContainer');}}" +
               "function nodeWalker(b) {[...b.children].forEach(c =>{setScrollClass(c);nodeWalker(c);})}" +
               "nodeWalker(document);let styles = document.createElement('style');" +
               "styles.innerText = '.isScrollableContainer::-webkit-scrollbar{width:" + scrollBarWidth + "px;height:" + scrollBarWidth + "px}' +" +
               "styles.classList.add('removeAfterComplete');document.body.appendChild(styles);function f() {" +
               "[...document.getElementsByClassName('isScrollableContainer')].forEach(e => {" +
               "let r = window.getComputedStyle(e)['display'];if ('block' !== r) {e.style.display = 'block';}" +
               "else {e.style.display = 'flex';}setTimeout(() => e.style.display = r, 100);})}f();";
    }

    static String getGlobalContainerScript() {
        return "(function () {let t = document.createElement('div');t.id = 'visualisationContainer';" +
               "t.innerHTML = '<style>#visualisationContainer{overflow:hidden;position:absolute;top:0;left:0;right:0;height:" + getMaxElementHeight() + "px;}</style>';" +
               "t.classList.add('removeAfterComplete');document.body.appendChild(t);}());";
    }

    private static long getMaxElementHeight() {
        return (Long) DriverUtils.executeJS(
                "function getMaxHeight() {let currentMax = {'value': 0};getMaxHeightWalker(document.body, currentMax);" +
                "return currentMax.value;}function getMaxHeightWalker(currentNode, currentMax){[...currentNode.children]" +
                ".forEach(c => {if ('scrollHeight' in c && c.scrollHeight > currentMax.value) {currentMax.value = c.scrollHeight;" +
                "}getMaxHeightWalker(c, currentMax);})}return getMaxHeight();");
    }

    static String getClearScript() {
        return "(function () {[...document.getElementsByClassName('removeAfterComplete')]" +
               ".forEach(e => e.parentNode.removeChild(e));[...document.getElementsByClassName('measuringTypeText')]" +
               ".forEach(e => e.classList.remove('measuringTypeText'));[...document.getElementsByClassName('layoutDecorationElement')]" +
               ".forEach(e => e.classList.remove('layoutDecorationElement'));}());";
    }

    static String getLangChangeScript() {
        return "(function () {document.getElementsByTagName('html')[0].setAttribute('lang','en');}());";
    }

    static String getBeforeAfterScript() {
        return "function beforeAfterWalker(node) {[...node.children].forEach(e => {try {if (window.getComputedStyle(e, ':after')" +
               ".getPropertyValue('content') !== 'none') {e.classList.add('layoutAfterElement');}" +
               "if (window.getComputedStyle(e, ':before').getPropertyValue('content') !== 'none') {" +
               "e.classList.add('layoutBeforeElement');}} catch (f) {}beforeAfterWalker(e);})}beforeAfterWalker(document.body);";
    }

    static String getTextMeasuringScript() {
        return "function f(arg) {let t = arg.innerText.replace(/[\\n\\s]/g, '').toLowerCase();" +
               "if (t === undefined || t.length === 0) {return;}[...arg.children]" +
               ".forEach(e => {let r = e.innerText; if (r === undefined || r.length === 0) {return;}" +
               "t = t.replace(r.replace(/[\\n\\s]/g, '').toLowerCase(), '');" +
               "f(e);});if (t.trim().length > 0) {arg.classList.add('measuringTypeText');}}f(document.body);";
    }

    static String getBlock(final String id, final DataSetElement dataSetElement, final ElementsColor color) {
        final ElementsColor activeColor = color == ElementsColor.DEFAULT ? dataSetElement
                .getMeasuringType()
                .getColor() : color;
        final int top = dataSetElement.getPosition().getTop() + dataSetElement.getContainer().getTop();
        final int left = dataSetElement.getPosition().getLeft() + dataSetElement.getContainer().getLeft();
        final String inlineCss = "position:absolute;" +
                                 "width:" + dataSetElement.getPosition().getWidth() + "px;" +
                                 "height:" + dataSetElement.getPosition().getHeight() + "px;" +
                                 "top:" + top + "px;" +
                                 "left:" + left + "px;" +
                                 getBackground(activeColor) + ";" +
                                 "border: 2px dotted " + activeColor.getColor() + ";" +
                                 "z-index:10000";
        return "let div = document.createElement('div'); div.style.cssText = '" +
               inlineCss + "';" +
               "div.id='id_" + id + "';" +
               "div.classList.add('detected-elements');" +
               "div.setAttribute('onclick','changeIgnoredState(event)');" +
               "document.getElementById('visualisationContainer').appendChild(div);";
    }

    private static String getBackground(final ElementsColor color) {
        final int gradientLines = 10;
        final String angle = color == ElementsColor.RED ? "45deg" : "-45deg";
        final String gradientElement = IntStream
                .range(0, gradientLines)
                .mapToObj(i -> ", transparent " + (100 * i / gradientLines) + "%," +
                               "transparent " + ((100 * (i + 1) / gradientLines) - 1) + "%," +
                               color.getColor() + " " + ((100 * (i + 1) / gradientLines) - 1) + "%," +
                               color.getColor() + " " + ((100 * (i + 1) / gradientLines)) + "%")
                .collect(Collectors.joining());
        return "background: linear-gradient(" + angle + gradientElement + ")";
    }
}
