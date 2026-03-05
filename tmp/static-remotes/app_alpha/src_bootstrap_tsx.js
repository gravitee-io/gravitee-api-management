"use strict";
(self["webpackChunkapp_alpha"] = self["webpackChunkapp_alpha"] || []).push([["src_bootstrap_tsx"], {
"./src/styles.css"(__unused_rspack_module, __webpack_exports__, __webpack_require__) {
__webpack_require__.r(__webpack_exports__);
// extracted by css-extract-rspack-plugin


},
"./src/app/app.tsx"(module, __webpack_exports__, __webpack_require__) {
__webpack_require__.r(__webpack_exports__);
__webpack_require__.d(__webpack_exports__, {
  App: () => (App),
  "default": () => (__rspack_default_export)
});
/* import */ var react_jsx_dev_runtime__rspack_import_0 = __webpack_require__("webpack/sharing/consume/default/react/jsx-dev-runtime/react/jsx-dev-runtime");
/* import */ var react_jsx_dev_runtime__rspack_import_0_default = /*#__PURE__*/__webpack_require__.n(react_jsx_dev_runtime__rspack_import_0);
/* import */ var _nx_welcome__rspack_import_1 = __webpack_require__("./src/app/nx-welcome.tsx");
/* import */ var _styles_css__rspack_import_2 = __webpack_require__("./src/styles.css");
/* provided dependency */ var $ReactRefreshRuntime$ = __webpack_require__("../node_modules/@rspack/plugin-react-refresh/client/reactRefresh.js");



function App() {
    return /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
        children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)(_nx_welcome__rspack_import_1["default"], {
            title: "app_alpha"
        }, void 0, false, {
            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/app.tsx",
            lineNumber: 7,
            columnNumber: 13
        }, this)
    }, void 0, false, {
        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/app.tsx",
        lineNumber: 6,
        columnNumber: 9
    }, this);
}
_c = App;
/* export default */ const __rspack_default_export = (App);
var _c;
$RefreshReg$(_c, "App");

function $RefreshSig$() { return $ReactRefreshRuntime$.createSignatureFunctionForTransform() }
function $RefreshReg$(type, id) { $ReactRefreshRuntime$.register(type, module.id + "_" + id) }
Promise.resolve().then(() => { $ReactRefreshRuntime$.refresh(module.id, module.hot) });


},
"./src/app/nx-welcome.tsx"(module, __webpack_exports__, __webpack_require__) {
__webpack_require__.r(__webpack_exports__);
__webpack_require__.d(__webpack_exports__, {
  NxWelcome: () => (NxWelcome),
  "default": () => (__rspack_default_export)
});
/* import */ var react_jsx_dev_runtime__rspack_import_0 = __webpack_require__("webpack/sharing/consume/default/react/jsx-dev-runtime/react/jsx-dev-runtime");
/* import */ var react_jsx_dev_runtime__rspack_import_0_default = /*#__PURE__*/__webpack_require__.n(react_jsx_dev_runtime__rspack_import_0);
/* provided dependency */ var $ReactRefreshRuntime$ = __webpack_require__("../node_modules/@rspack/plugin-react-refresh/client/reactRefresh.js");

/*
 * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 This is a starter component and can be deleted.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 Delete this file and get started with your project!
 * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */ function NxWelcome(param) {
    var title = param.title;
    return /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)(react_jsx_dev_runtime__rspack_import_0.Fragment, {
        children: [
            /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("style", {
                dangerouslySetInnerHTML: {
                    __html: "\n    html {\n      -webkit-text-size-adjust: 100%;\n      font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont,\n      'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif,\n      'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',\n      'Noto Color Emoji';\n      line-height: 1.5;\n      tab-size: 4;\n      scroll-behavior: smooth;\n    }\n    body {\n      font-family: inherit;\n      line-height: inherit;\n      margin: 0;\n    }\n    h1,\n    h2,\n    p,\n    pre {\n      margin: 0;\n    }\n    *,\n    ::before,\n    ::after {\n      box-sizing: border-box;\n      border-width: 0;\n      border-style: solid;\n      border-color: currentColor;\n    }\n    h1,\n    h2 {\n      font-size: inherit;\n      font-weight: inherit;\n    }\n    a {\n      color: inherit;\n      text-decoration: inherit;\n    }\n    pre {\n      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas,\n      'Liberation Mono', 'Courier New', monospace;\n    }\n    svg {\n      display: block;\n      vertical-align: middle;\n      shape-rendering: auto;\n      text-rendering: optimizeLegibility;\n    }\n    pre {\n      background-color: rgba(55, 65, 81, 1);\n      border-radius: 0.25rem;\n      color: rgba(229, 231, 235, 1);\n      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas,\n      'Liberation Mono', 'Courier New', monospace;\n      overflow: auto;\n      padding: 0.5rem 0.75rem;\n    }\n\n    .shadow {\n      box-shadow: 0 0 #0000, 0 0 #0000, 0 10px 15px -3px rgba(0, 0, 0, 0.1),\n      0 4px 6px -2px rgba(0, 0, 0, 0.05);\n    }\n    .rounded {\n      border-radius: 1.5rem;\n    }\n    .wrapper {\n      width: 100%;\n    }\n    .container {\n      margin-left: auto;\n      margin-right: auto;\n      max-width: 768px;\n      padding-bottom: 3rem;\n      padding-left: 1rem;\n      padding-right: 1rem;\n      color: rgba(55, 65, 81, 1);\n      width: 100%;\n    }\n    #welcome {\n      margin-top: 2.5rem;\n    }\n    #welcome h1 {\n      font-size: 3rem;\n      font-weight: 500;\n      letter-spacing: -0.025em;\n      line-height: 1;\n    }\n    #welcome span {\n      display: block;\n      font-size: 1.875rem;\n      font-weight: 300;\n      line-height: 2.25rem;\n      margin-bottom: 0.5rem;\n    }\n    #hero {\n      align-items: center;\n      background-color: hsla(214, 62%, 21%, 1);\n      border: none;\n      box-sizing: border-box;\n      color: rgba(55, 65, 81, 1);\n      display: grid;\n      grid-template-columns: 1fr;\n      margin-top: 3.5rem;\n    }\n    #hero .text-container {\n      color: rgba(255, 255, 255, 1);\n      padding: 3rem 2rem;\n    }\n    #hero .text-container h2 {\n      font-size: 1.5rem;\n      line-height: 2rem;\n      position: relative;\n    }\n    #hero .text-container h2 svg {\n      color: hsla(162, 47%, 50%, 1);\n      height: 2rem;\n      left: -0.25rem;\n      position: absolute;\n      top: 0;\n      width: 2rem;\n    }\n    #hero .text-container h2 span {\n      margin-left: 2.5rem;\n    }\n    #hero .text-container a {\n      background-color: rgba(255, 255, 255, 1);\n      border-radius: 0.75rem;\n      color: rgba(55, 65, 81, 1);\n      display: inline-block;\n      margin-top: 1.5rem;\n      padding: 1rem 2rem;\n      text-decoration: inherit;\n    }\n    #hero .logo-container {\n      display: none;\n      justify-content: center;\n      padding-left: 2rem;\n      padding-right: 2rem;\n    }\n    #hero .logo-container svg {\n      color: rgba(255, 255, 255, 1);\n      width: 66.666667%;\n    }\n    #middle-content {\n      align-items: flex-start;\n      display: grid;\n      grid-template-columns: 1fr;\n      margin-top: 3.5rem;\n    }\n\n    #middle-content #middle-content-container {\n      display: flex;\n      flex-direction: column;\n      gap: 2rem;\n    }\n    #learning-materials {\n      padding: 2.5rem 2rem;\n    }\n    #learning-materials h2 {\n      font-weight: 500;\n      font-size: 1.25rem;\n      letter-spacing: -0.025em;\n      line-height: 1.75rem;\n      padding-left: 1rem;\n      padding-right: 1rem;\n    }\n    .list-item-link {\n      align-items: center;\n      border-radius: 0.75rem;\n      display: flex;\n      margin-top: 1rem;\n      padding: 1rem;\n      transition-property: background-color, border-color, color, fill, stroke,\n      opacity, box-shadow, transform, filter, backdrop-filter,\n      -webkit-backdrop-filter;\n      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);\n      transition-duration: 150ms;\n      width: 100%;\n    }\n    .list-item-link svg:first-child {\n      margin-right: 1rem;\n      height: 1.5rem;\n      transition-property: background-color, border-color, color, fill, stroke,\n      opacity, box-shadow, transform, filter, backdrop-filter,\n      -webkit-backdrop-filter;\n      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);\n      transition-duration: 150ms;\n      width: 1.5rem;\n    }\n    .list-item-link > span {\n      flex-grow: 1;\n      font-weight: 400;\n      transition-property: background-color, border-color, color, fill, stroke,\n      opacity, box-shadow, transform, filter, backdrop-filter,\n      -webkit-backdrop-filter;\n      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);\n    }\n    .list-item-link > span > span {\n      color: rgba(107, 114, 128, 1);\n      display: block;\n      flex-grow: 1;\n      font-size: 0.75rem;\n      font-weight: 300;\n      line-height: 1rem;\n      transition-property: background-color, border-color, color, fill, stroke,\n      opacity, box-shadow, transform, filter, backdrop-filter,\n      -webkit-backdrop-filter;\n      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);\n    }\n    .list-item-link svg:last-child {\n      height: 1rem;\n      transition-property: all;\n      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);\n      transition-duration: 150ms;\n      width: 1rem;\n    }\n    .list-item-link:hover {\n      color: rgba(255, 255, 255, 1);\n      background-color: hsla(162, 55%, 33%, 1);\n    }\n    .list-item-link:hover > span {}\n    .list-item-link:hover > span > span {\n      color: rgba(243, 244, 246, 1);\n    }\n    .list-item-link:hover svg:last-child {\n      transform: translateX(0.25rem);\n    }\n    #other-links {}\n    .button-pill {\n      padding: 1.5rem 2rem;\n      margin-bottom: 2rem;\n      transition-duration: 300ms;\n      transition-property: background-color, border-color, color, fill, stroke,\n      opacity, box-shadow, transform, filter, backdrop-filter,\n      -webkit-backdrop-filter;\n      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);\n      align-items: center;\n      display: flex;\n    }\n    .button-pill svg {\n      transition-property: background-color, border-color, color, fill, stroke,\n      opacity, box-shadow, transform, filter, backdrop-filter,\n      -webkit-backdrop-filter;\n      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);\n      transition-duration: 150ms;\n      flex-shrink: 0;\n      width: 3rem;\n    }\n    .button-pill > span {\n      letter-spacing: -0.025em;\n      font-weight: 400;\n      font-size: 1.125rem;\n      line-height: 1.75rem;\n      padding-left: 1rem;\n      padding-right: 1rem;\n    }\n    .button-pill span span {\n      display: block;\n      font-size: 0.875rem;\n      font-weight: 300;\n      line-height: 1.25rem;\n    }\n    .button-pill:hover svg,\n    .button-pill:hover {\n      color: rgba(255, 255, 255, 1) !important;\n    }\n    #nx-console:hover {\n      background-color: rgba(0, 122, 204, 1);\n    }\n    #nx-console svg {\n      color: rgba(0, 122, 204, 1);\n    }\n    #nx-console-jetbrains {\n      margin-top: 2rem;\n    }\n    #nx-console-jetbrains:hover {\n      background-color: rgba(255, 49, 140, 1);\n    }\n    #nx-console-jetbrains svg {\n      color: rgba(255, 49, 140, 1);\n    }\n    #nx-repo:hover {\n      background-color: rgba(24, 23, 23, 1);\n    }\n    #nx-repo svg {\n      color: rgba(24, 23, 23, 1);\n    }\n    #nx-cloud {\n      margin-bottom: 2rem;\n      margin-top: 2rem;\n      padding: 2.5rem 2rem;\n    }\n    #nx-cloud > div {\n      align-items: center;\n      display: flex;\n    }\n    #nx-cloud > div svg {\n      border-radius: 0.375rem;\n      flex-shrink: 0;\n      width: 3rem;\n    }\n    #nx-cloud > div h2 {\n      font-size: 1.125rem;\n      font-weight: 400;\n      letter-spacing: -0.025em;\n      line-height: 1.75rem;\n      padding-left: 1rem;\n      padding-right: 1rem;\n    }\n    #nx-cloud > div h2 span {\n      display: block;\n      font-size: 0.875rem;\n      font-weight: 300;\n      line-height: 1.25rem;\n    }\n    #nx-cloud p {\n      font-size: 1rem;\n      line-height: 1.5rem;\n      margin-top: 1rem;\n    }\n    #nx-cloud pre {\n      margin-top: 1rem;\n    }\n    #nx-cloud a {\n      color: rgba(107, 114, 128, 1);\n      display: block;\n      font-size: 0.875rem;\n      line-height: 1.25rem;\n      margin-top: 1.5rem;\n      text-align: right;\n    }\n    #nx-cloud a:hover {\n      text-decoration: underline;\n    }\n    #commands {\n      padding: 2.5rem 2rem;\n      margin-top: 3.5rem;\n    }\n    #commands h2 {\n      font-size: 1.25rem;\n      font-weight: 400;\n      letter-spacing: -0.025em;\n      line-height: 1.75rem;\n      padding-left: 1rem;\n      padding-right: 1rem;\n    }\n    #commands p {\n      font-size: 1rem;\n      font-weight: 300;\n      line-height: 1.5rem;\n      margin-top: 1rem;\n      padding-left: 1rem;\n      padding-right: 1rem;\n    }\n    details {\n      align-items: center;\n      display: flex;\n      margin-top: 1rem;\n      padding-left: 1rem;\n      padding-right: 1rem;\n      width: 100%;\n    }\n    details pre > span {\n      color: rgba(181, 181, 181, 1);\n      display: block;\n    }\n    summary {\n      border-radius: 0.5rem;\n      display: flex;\n      font-weight: 400;\n      padding: 0.5rem;\n      cursor: pointer;\n      transition-property: background-color, border-color, color, fill, stroke,\n      opacity, box-shadow, transform, filter, backdrop-filter,\n      -webkit-backdrop-filter;\n      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);\n      transition-duration: 150ms;\n    }\n    summary:hover {\n      background-color: rgba(243, 244, 246, 1);\n    }\n    summary svg {\n      height: 1.5rem;\n      margin-right: 1rem;\n      width: 1.5rem;\n    }\n    #love {\n      color: rgba(107, 114, 128, 1);\n      font-size: 0.875rem;\n      line-height: 1.25rem;\n      margin-top: 3.5rem;\n      opacity: 0.6;\n      text-align: center;\n    }\n    #love svg {\n      color: rgba(252, 165, 165, 1);\n      width: 1.25rem;\n      height: 1.25rem;\n      display: inline;\n      margin-top: -0.25rem;\n    }\n    @media screen and (min-width: 768px) {\n      #hero {\n        grid-template-columns: repeat(2, minmax(0, 1fr));\n      }\n      #hero .logo-container {\n        display: flex;\n      }\n      #middle-content {\n        grid-template-columns: repeat(2, minmax(0, 1fr));\n        gap: 4rem;\n      }\n    }\n          "
                }
            }, void 0, false, {
                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                lineNumber: 11,
                columnNumber: 13
            }, this),
            /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                className: "wrapper",
                children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                    className: "container",
                    children: [
                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                            id: "welcome",
                            children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("h1", {
                                children: [
                                    /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                        children: " Hello there, "
                                    }, void 0, false, {
                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                        lineNumber: 434,
                                        columnNumber: 29
                                    }, this),
                                    "Welcome ",
                                    title,
                                    " \uD83D\uDC4B"
                                ]
                            }, void 0, true, {
                                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                lineNumber: 433,
                                columnNumber: 25
                            }, this)
                        }, void 0, false, {
                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                            lineNumber: 432,
                            columnNumber: 21
                        }, this),
                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                            id: "hero",
                            className: "rounded",
                            children: [
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                                    className: "text-container",
                                    children: [
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("h2", {
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                    fill: "none",
                                                    stroke: "currentColor",
                                                    viewBox: "0 0 24 24",
                                                    xmlns: "http://www.w3.org/2000/svg",
                                                    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                        strokeLinecap: "round",
                                                        strokeLinejoin: "round",
                                                        strokeWidth: "2",
                                                        d: "M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z"
                                                    }, void 0, false, {
                                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                        lineNumber: 443,
                                                        columnNumber: 37
                                                    }, this)
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 442,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: "You're up and running"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 450,
                                                    columnNumber: 33
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 441,
                                            columnNumber: 29
                                        }, this),
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                            href: "#commands",
                                            children: " What's next? "
                                        }, void 0, false, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 452,
                                            columnNumber: 29
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 440,
                                    columnNumber: 25
                                }, this),
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                                    className: "logo-container",
                                    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                        fill: "currentColor",
                                        role: "img",
                                        viewBox: "0 0 24 24",
                                        xmlns: "http://www.w3.org/2000/svg",
                                        children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                            d: "M11.987 14.138l-3.132 4.923-5.193-8.427-.012 8.822H0V4.544h3.691l5.247 8.833.005-3.998 3.044 4.759zm.601-5.761c.024-.048 0-3.784.008-3.833h-3.65c.002.059-.005 3.776-.003 3.833h3.645zm5.634 4.134a2.061 2.061 0 0 0-1.969 1.336 1.963 1.963 0 0 1 2.343-.739c.396.161.917.422 1.33.283a2.1 2.1 0 0 0-1.704-.88zm3.39 1.061c-.375-.13-.8-.277-1.109-.681-.06-.08-.116-.17-.176-.265a2.143 2.143 0 0 0-.533-.642c-.294-.216-.68-.322-1.18-.322a2.482 2.482 0 0 0-2.294 1.536 2.325 2.325 0 0 1 4.002.388.75.75 0 0 0 .836.334c.493-.105.46.36 1.203.518v-.133c-.003-.446-.246-.55-.75-.733zm2.024 1.266a.723.723 0 0 0 .347-.638c-.01-2.957-2.41-5.487-5.37-5.487a5.364 5.364 0 0 0-4.487 2.418c-.01-.026-1.522-2.39-1.538-2.418H8.943l3.463 5.423-3.379 5.32h3.54l1.54-2.366 1.568 2.366h3.541l-3.21-5.052a.7.7 0 0 1-.084-.32 2.69 2.69 0 0 1 2.69-2.691h.001c1.488 0 1.736.89 2.057 1.308.634.826 1.9.464 1.9 1.541a.707.707 0 0 0 1.066.596zm.35.133c-.173.372-.56.338-.755.639-.176.271.114.412.114.412s.337.156.538-.311c.104-.231.14-.488.103-.74z"
                                        }, void 0, false, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 456,
                                            columnNumber: 33
                                        }, this)
                                    }, void 0, false, {
                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                        lineNumber: 455,
                                        columnNumber: 29
                                    }, this)
                                }, void 0, false, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 454,
                                    columnNumber: 25
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                            lineNumber: 439,
                            columnNumber: 21
                        }, this),
                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                            id: "middle-content",
                            children: [
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                                    id: "middle-content-container",
                                    children: [
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                                            id: "learning-materials",
                                            className: "rounded shadow",
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("h2", {
                                                    children: "Learning materials"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 464,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                                    href: "https://nx.dev/getting-started/intro?utm_source=nx-project",
                                                    target: "_blank",
                                                    rel: "noreferrer",
                                                    className: "list-item-link",
                                                    children: [
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            fill: "none",
                                                            stroke: "currentColor",
                                                            viewBox: "0 0 24 24",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                strokeLinecap: "round",
                                                                strokeLinejoin: "round",
                                                                strokeWidth: "2",
                                                                d: "M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                                                            }, void 0, false, {
                                                                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                lineNumber: 472,
                                                                columnNumber: 41
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 471,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                            children: [
                                                                "Documentation",
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                                    children: " Everything is in there "
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 481,
                                                                    columnNumber: 41
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 479,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            fill: "none",
                                                            stroke: "currentColor",
                                                            viewBox: "0 0 24 24",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                strokeLinecap: "round",
                                                                strokeLinejoin: "round",
                                                                strokeWidth: "2",
                                                                d: "M9 5l7 7-7 7"
                                                            }, void 0, false, {
                                                                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                lineNumber: 484,
                                                                columnNumber: 41
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 483,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 465,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                                    href: "https://nx.dev/blog/?utm_source=nx-project",
                                                    target: "_blank",
                                                    rel: "noreferrer",
                                                    className: "list-item-link",
                                                    children: [
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            fill: "none",
                                                            stroke: "currentColor",
                                                            viewBox: "0 0 24 24",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                strokeLinecap: "round",
                                                                strokeLinejoin: "round",
                                                                strokeWidth: "2",
                                                                d: "M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z"
                                                            }, void 0, false, {
                                                                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                lineNumber: 494,
                                                                columnNumber: 41
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 493,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                            children: [
                                                                "Blog",
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                                    children: " Changelog, features & events "
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 503,
                                                                    columnNumber: 41
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 501,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            fill: "none",
                                                            stroke: "currentColor",
                                                            viewBox: "0 0 24 24",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                strokeLinecap: "round",
                                                                strokeLinejoin: "round",
                                                                strokeWidth: "2",
                                                                d: "M9 5l7 7-7 7"
                                                            }, void 0, false, {
                                                                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                lineNumber: 506,
                                                                columnNumber: 41
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 505,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 487,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                                    href: "https://www.youtube.com/@NxDevtools/videos?utm_source=nx-project&sub_confirmation=1",
                                                    target: "_blank",
                                                    rel: "noreferrer",
                                                    className: "list-item-link",
                                                    children: [
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            role: "img",
                                                            viewBox: "0 0 24 24",
                                                            fill: "currentColor",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            children: [
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("title", {
                                                                    children: "YouTube"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 516,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 517,
                                                                    columnNumber: 41
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 515,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                            children: [
                                                                "YouTube channel",
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                                    children: " Nx Show, talks & tutorials "
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 521,
                                                                    columnNumber: 41
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 519,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            fill: "none",
                                                            stroke: "currentColor",
                                                            viewBox: "0 0 24 24",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                strokeLinecap: "round",
                                                                strokeLinejoin: "round",
                                                                strokeWidth: "2",
                                                                d: "M9 5l7 7-7 7"
                                                            }, void 0, false, {
                                                                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                lineNumber: 524,
                                                                columnNumber: 41
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 523,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 509,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                                    href: "https://nx.dev/react-tutorial/1-code-generation?utm_source=nx-project",
                                                    target: "_blank",
                                                    rel: "noreferrer",
                                                    className: "list-item-link",
                                                    children: [
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            fill: "none",
                                                            stroke: "currentColor",
                                                            viewBox: "0 0 24 24",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                strokeLinecap: "round",
                                                                strokeLinejoin: "round",
                                                                strokeWidth: "2",
                                                                d: "M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122"
                                                            }, void 0, false, {
                                                                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                lineNumber: 534,
                                                                columnNumber: 41
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 533,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                            children: [
                                                                "Interactive tutorials",
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                                    children: " Create an app, step-by-step "
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 543,
                                                                    columnNumber: 41
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 541,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            fill: "none",
                                                            stroke: "currentColor",
                                                            viewBox: "0 0 24 24",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                strokeLinecap: "round",
                                                                strokeLinejoin: "round",
                                                                strokeWidth: "2",
                                                                d: "M9 5l7 7-7 7"
                                                            }, void 0, false, {
                                                                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                lineNumber: 546,
                                                                columnNumber: 41
                                                            }, this)
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 545,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 527,
                                                    columnNumber: 33
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 463,
                                            columnNumber: 29
                                        }, this),
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                            id: "nx-repo",
                                            className: "button-pill rounded shadow",
                                            href: "https://github.com/nrwl/nx?utm_source=nx-project",
                                            target: "_blank",
                                            rel: "noreferrer",
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                    fill: "currentColor",
                                                    role: "img",
                                                    viewBox: "0 0 24 24",
                                                    xmlns: "http://www.w3.org/2000/svg",
                                                    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                        d: "M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"
                                                    }, void 0, false, {
                                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                        lineNumber: 558,
                                                        columnNumber: 37
                                                    }, this)
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 557,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: [
                                                        "Nx is open source",
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                            children: " Love Nx? Give us a star! "
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 562,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 560,
                                                    columnNumber: 33
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 550,
                                            columnNumber: 29
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 462,
                                    columnNumber: 25
                                }, this),
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                                    id: "other-links",
                                    children: [
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                            id: "nx-console",
                                            className: "button-pill rounded shadow",
                                            href: "https://marketplace.visualstudio.com/items?itemName=nrwl.angular-console&utm_source=nx-project",
                                            target: "_blank",
                                            rel: "noreferrer",
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                    fill: "currentColor",
                                                    role: "img",
                                                    viewBox: "0 0 24 24",
                                                    xmlns: "http://www.w3.org/2000/svg",
                                                    children: [
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("title", {
                                                            children: "Visual Studio Code"
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 575,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                            d: "M23.15 2.587L18.21.21a1.494 1.494 0 0 0-1.705.29l-9.46 8.63-4.12-3.128a.999.999 0 0 0-1.276.057L.327 7.261A1 1 0 0 0 .326 8.74L3.899 12 .326 15.26a1 1 0 0 0 .001 1.479L1.65 17.94a.999.999 0 0 0 1.276.057l4.12-3.128 9.46 8.63a1.492 1.492 0 0 0 1.704.29l4.942-2.377A1.5 1.5 0 0 0 24 20.06V3.939a1.5 1.5 0 0 0-.85-1.352zm-5.146 14.861L10.826 12l7.178-5.448v10.896z"
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 576,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 574,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: [
                                                        "Install Nx Console for VSCode",
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                            children: "The official VSCode extension for Nx."
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 580,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 578,
                                                    columnNumber: 33
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 567,
                                            columnNumber: 29
                                        }, this),
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                            id: "nx-console-jetbrains",
                                            className: "button-pill rounded shadow",
                                            href: "https://plugins.jetbrains.com/plugin/21060-nx-console",
                                            target: "_blank",
                                            rel: "noreferrer",
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                    height: "48",
                                                    width: "48",
                                                    viewBox: "20 20 60 60",
                                                    xmlns: "http://www.w3.org/2000/svg",
                                                    children: [
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                            d: "m22.5 22.5h60v60h-60z"
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 591,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("g", {
                                                            fill: "#fff",
                                                            children: [
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m29.03 71.25h22.5v3.75h-22.5z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 593,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m28.09 38 1.67-1.58a1.88 1.88 0 0 0 1.47.87c.64 0 1.06-.44 1.06-1.31v-5.98h2.58v6a3.48 3.48 0 0 1 -.87 2.6 3.56 3.56 0 0 1 -2.57.95 3.84 3.84 0 0 1 -3.34-1.55z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 594,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m36 30h7.53v2.19h-5v1.44h4.49v2h-4.42v1.49h5v2.21h-7.6z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 595,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m47.23 32.29h-2.8v-2.29h8.21v2.27h-2.81v7.1h-2.6z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 596,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m29.13 43.08h4.42a3.53 3.53 0 0 1 2.55.83 2.09 2.09 0 0 1 .6 1.53 2.16 2.16 0 0 1 -1.44 2.09 2.27 2.27 0 0 1 1.86 2.29c0 1.61-1.31 2.59-3.55 2.59h-4.44zm5 2.89c0-.52-.42-.8-1.18-.8h-1.29v1.64h1.24c.79 0 1.25-.26 1.25-.81zm-.9 2.66h-1.57v1.73h1.62c.8 0 1.24-.31 1.24-.86 0-.5-.4-.87-1.27-.87z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 597,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m38 43.08h4.1a4.19 4.19 0 0 1 3 1 2.93 2.93 0 0 1 .9 2.19 3 3 0 0 1 -1.93 2.89l2.24 3.27h-3l-1.88-2.84h-.87v2.84h-2.56zm4 4.5c.87 0 1.39-.43 1.39-1.11 0-.75-.54-1.12-1.4-1.12h-1.44v2.26z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 598,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m49.59 43h2.5l4 9.44h-2.79l-.67-1.69h-3.63l-.67 1.69h-2.71zm2.27 5.73-1-2.65-1.06 2.65z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 599,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m56.46 43.05h2.6v9.37h-2.6z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 600,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m60.06 43.05h2.42l3.37 5v-5h2.57v9.37h-2.26l-3.53-5.14v5.14h-2.57z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 601,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    d: "m68.86 51 1.45-1.73a4.84 4.84 0 0 0 3 1.13c.71 0 1.08-.24 1.08-.65 0-.4-.31-.6-1.59-.91-2-.46-3.53-1-3.53-2.93 0-1.74 1.37-3 3.62-3a5.89 5.89 0 0 1 3.86 1.25l-1.26 1.84a4.63 4.63 0 0 0 -2.62-.92c-.63 0-.94.25-.94.6 0 .42.32.61 1.63.91 2.14.46 3.44 1.16 3.44 2.91 0 1.91-1.51 3-3.79 3a6.58 6.58 0 0 1 -4.35-1.5z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 602,
                                                                    columnNumber: 41
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 592,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 590,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: [
                                                        "Install Nx Console for JetBrains",
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                            children: "Available for WebStorm, Intellij IDEA Ultimate and more!"
                                                        }, void 0, false, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 607,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 605,
                                                    columnNumber: 33
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 583,
                                            columnNumber: 29
                                        }, this),
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                                            id: "nx-cloud",
                                            className: "rounded shadow",
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                                                    children: [
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                            id: "nx-cloud-logo",
                                                            role: "img",
                                                            xmlns: "http://www.w3.org/2000/svg",
                                                            stroke: "currentColor",
                                                            fill: "transparent",
                                                            viewBox: "0 0 24 24",
                                                            children: [
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    strokeWidth: "2",
                                                                    d: "M23 3.75V6.5c-3.036 0-5.5 2.464-5.5 5.5s-2.464 5.5-5.5 5.5-5.5 2.464-5.5 5.5H3.75C2.232 23 1 21.768 1 20.25V3.75C1 2.232 2.232 1 3.75 1h16.5C21.768 1 23 2.232 23 3.75Z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 620,
                                                                    columnNumber: 41
                                                                }, this),
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                                    strokeWidth: "2",
                                                                    d: "M23 6v14.1667C23 21.7307 21.7307 23 20.1667 23H6c0-3.128 2.53867-5.6667 5.6667-5.6667 3.128 0 5.6666-2.5386 5.6666-5.6666C17.3333 8.53867 19.872 6 23 6Z"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 624,
                                                                    columnNumber: 41
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 612,
                                                            columnNumber: 37
                                                        }, this),
                                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("h2", {
                                                            children: [
                                                                "Nx Cloud",
                                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                                    children: "Enable faster CI & better DX"
                                                                }, void 0, false, {
                                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                                    lineNumber: 631,
                                                                    columnNumber: 41
                                                                }, this)
                                                            ]
                                                        }, void 0, true, {
                                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                            lineNumber: 629,
                                                            columnNumber: 37
                                                        }, this)
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 611,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("p", {
                                                    children: "You can activate distributed tasks executions and caching by running:"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 634,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("pre", {
                                                    children: "nx connect"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 635,
                                                    columnNumber: 33
                                                }, this),
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("a", {
                                                    href: "https://nx.dev/nx-cloud?utm_source=nx-project",
                                                    target: "_blank",
                                                    rel: "noreferrer",
                                                    children: [
                                                        ' ',
                                                        "What is Nx Cloud?",
                                                        ' '
                                                    ]
                                                }, void 0, true, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 636,
                                                    columnNumber: 33
                                                }, this)
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 610,
                                            columnNumber: 29
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 566,
                                    columnNumber: 25
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                            lineNumber: 461,
                            columnNumber: 21
                        }, this),
                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("div", {
                            id: "commands",
                            className: "rounded shadow",
                            children: [
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("h2", {
                                    children: "Next steps"
                                }, void 0, false, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 645,
                                    columnNumber: 25
                                }, this),
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("p", {
                                    children: "Here are some things you can do with Nx:"
                                }, void 0, false, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 646,
                                    columnNumber: 25
                                }, this),
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("details", {
                                    children: [
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("summary", {
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                    fill: "none",
                                                    stroke: "currentColor",
                                                    viewBox: "0 0 24 24",
                                                    xmlns: "http://www.w3.org/2000/svg",
                                                    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                        strokeLinecap: "round",
                                                        strokeLinejoin: "round",
                                                        strokeWidth: "2",
                                                        d: "M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                                                    }, void 0, false, {
                                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                        lineNumber: 650,
                                                        columnNumber: 37
                                                    }, this)
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 649,
                                                    columnNumber: 33
                                                }, this),
                                                "Build, test and lint your app"
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 648,
                                            columnNumber: 29
                                        }, this),
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("pre", {
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: "# Build"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 660,
                                                    columnNumber: 33
                                                }, this),
                                                "nx build ",
                                                title,
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: "# Test"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 662,
                                                    columnNumber: 33
                                                }, this),
                                                "nx test ",
                                                title,
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: "# Lint"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 664,
                                                    columnNumber: 33
                                                }, this),
                                                "nx lint ",
                                                title,
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: "# Run them together!"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 666,
                                                    columnNumber: 33
                                                }, this),
                                                "nx run-many -p ",
                                                title,
                                                " -t build test lint"
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 659,
                                            columnNumber: 29
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 647,
                                    columnNumber: 25
                                }, this),
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("details", {
                                    children: [
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("summary", {
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                    fill: "none",
                                                    stroke: "currentColor",
                                                    viewBox: "0 0 24 24",
                                                    xmlns: "http://www.w3.org/2000/svg",
                                                    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                        strokeLinecap: "round",
                                                        strokeLinejoin: "round",
                                                        strokeWidth: "2",
                                                        d: "M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                                                    }, void 0, false, {
                                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                        lineNumber: 674,
                                                        columnNumber: 37
                                                    }, this)
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 673,
                                                    columnNumber: 33
                                                }, this),
                                                "View project details"
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 672,
                                            columnNumber: 29
                                        }, this),
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("pre", {
                                            children: [
                                                "nx show project ",
                                                title
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 683,
                                            columnNumber: 29
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 671,
                                    columnNumber: 25
                                }, this),
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("details", {
                                    children: [
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("summary", {
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                    fill: "none",
                                                    stroke: "currentColor",
                                                    viewBox: "0 0 24 24",
                                                    xmlns: "http://www.w3.org/2000/svg",
                                                    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                        strokeLinecap: "round",
                                                        strokeLinejoin: "round",
                                                        strokeWidth: "2",
                                                        d: "M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                                                    }, void 0, false, {
                                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                        lineNumber: 688,
                                                        columnNumber: 37
                                                    }, this)
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 687,
                                                    columnNumber: 33
                                                }, this),
                                                "View interactive project graph"
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 686,
                                            columnNumber: 29
                                        }, this),
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("pre", {
                                            children: "nx graph"
                                        }, void 0, false, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 697,
                                            columnNumber: 29
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 685,
                                    columnNumber: 25
                                }, this),
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("details", {
                                    children: [
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("summary", {
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                                    fill: "none",
                                                    stroke: "currentColor",
                                                    viewBox: "0 0 24 24",
                                                    xmlns: "http://www.w3.org/2000/svg",
                                                    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                                        strokeLinecap: "round",
                                                        strokeLinejoin: "round",
                                                        strokeWidth: "2",
                                                        d: "M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                                                    }, void 0, false, {
                                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                        lineNumber: 702,
                                                        columnNumber: 37
                                                    }, this)
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 701,
                                                    columnNumber: 33
                                                }, this),
                                                "Add UI library"
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 700,
                                            columnNumber: 29
                                        }, this),
                                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("pre", {
                                            children: [
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: "# Generate UI lib"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 712,
                                                    columnNumber: 33
                                                }, this),
                                                "nx g @nx/react:lib ui",
                                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("span", {
                                                    children: "# Add a component"
                                                }, void 0, false, {
                                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                                    lineNumber: 714,
                                                    columnNumber: 33
                                                }, this),
                                                "nx g @nx/react:component ui/src/lib/button"
                                            ]
                                        }, void 0, true, {
                                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                            lineNumber: 711,
                                            columnNumber: 29
                                        }, this)
                                    ]
                                }, void 0, true, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 699,
                                    columnNumber: 25
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                            lineNumber: 644,
                            columnNumber: 21
                        }, this),
                        /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("p", {
                            id: "love",
                            children: [
                                "Carefully crafted with",
                                /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("svg", {
                                    fill: "currentColor",
                                    stroke: "none",
                                    viewBox: "0 0 24 24",
                                    xmlns: "http://www.w3.org/2000/svg",
                                    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)("path", {
                                        strokeLinecap: "round",
                                        strokeLinejoin: "round",
                                        strokeWidth: "2",
                                        d: "M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                                    }, void 0, false, {
                                        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                        lineNumber: 723,
                                        columnNumber: 29
                                    }, this)
                                }, void 0, false, {
                                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                                    lineNumber: 722,
                                    columnNumber: 25
                                }, this)
                            ]
                        }, void 0, true, {
                            fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                            lineNumber: 720,
                            columnNumber: 21
                        }, this)
                    ]
                }, void 0, true, {
                    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                    lineNumber: 431,
                    columnNumber: 17
                }, this)
            }, void 0, false, {
                fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/app/nx-welcome.tsx",
                lineNumber: 430,
                columnNumber: 13
            }, this)
        ]
    }, void 0, true);
}
_c = NxWelcome;
/* export default */ const __rspack_default_export = (NxWelcome);
var _c;
$RefreshReg$(_c, "NxWelcome");

function $RefreshSig$() { return $ReactRefreshRuntime$.createSignatureFunctionForTransform() }
function $RefreshReg$(type, id) { $ReactRefreshRuntime$.register(type, module.id + "_" + id) }
Promise.resolve().then(() => { $ReactRefreshRuntime$.refresh(module.id, module.hot) });


},
"./src/bootstrap.tsx"(module, __webpack_exports__, __webpack_require__) {
__webpack_require__.r(__webpack_exports__);
/* import */ var react_jsx_dev_runtime__rspack_import_0 = __webpack_require__("webpack/sharing/consume/default/react/jsx-dev-runtime/react/jsx-dev-runtime");
/* import */ var react_jsx_dev_runtime__rspack_import_0_default = /*#__PURE__*/__webpack_require__.n(react_jsx_dev_runtime__rspack_import_0);
/* import */ var react__rspack_import_1 = __webpack_require__("webpack/sharing/consume/default/react/react");
/* import */ var react__rspack_import_1_default = /*#__PURE__*/__webpack_require__.n(react__rspack_import_1);
/* import */ var react_dom_client__rspack_import_2 = __webpack_require__("webpack/sharing/consume/default/react-dom/client/react-dom/client");
/* import */ var react_dom_client__rspack_import_2_default = /*#__PURE__*/__webpack_require__.n(react_dom_client__rspack_import_2);
/* import */ var _app_app__rspack_import_3 = __webpack_require__("./src/app/app.tsx");
/* provided dependency */ var $ReactRefreshRuntime$ = __webpack_require__("../node_modules/@rspack/plugin-react-refresh/client/reactRefresh.js");




var root = react_dom_client__rspack_import_2.createRoot(document.getElementById('root'));
root.render(/*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)(react__rspack_import_1.StrictMode, {
    children: /*#__PURE__*/ (0,react_jsx_dev_runtime__rspack_import_0.jsxDEV)(_app_app__rspack_import_3["default"], {}, void 0, false, {
        fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/bootstrap.tsx",
        lineNumber: 8,
        columnNumber: 9
    }, undefined)
}, void 0, false, {
    fileName: "/Users/jourdiwaller/Documents/git/gravitee-api-management/app_alpha/src/bootstrap.tsx",
    lineNumber: 7,
    columnNumber: 5
}, undefined));

function $RefreshSig$() { return $ReactRefreshRuntime$.createSignatureFunctionForTransform() }
function $RefreshReg$(type, id) { $ReactRefreshRuntime$.register(type, module.id + "_" + id) }
Promise.resolve().then(() => { $ReactRefreshRuntime$.refresh(module.id, module.hot) });


},

}]);
//# sourceMappingURL=src_bootstrap_tsx.js.map