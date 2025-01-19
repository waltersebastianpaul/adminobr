
/*
* Index
* @version: 1.0.0
* @author: Keenthemes
* Copyright 2023 Keenthemes
*/

import KTUtils from './helpers/utils';
import KTDom from './helpers/dom';
import KTEventHandler from './helpers/event-handler';
import KTMenu from './components/menu';
import KTDropdown from './components/dropdown';
import KTModal from './components/modal';
import KTDrawer from './components/drawer';
import KTCollapse from './components/collapse';
import KTDismiss from './components/dismiss';
import KTTabs from './components/tabs';
import KTAccordion from './components/accordion';
import KTScrollspy from './components/scrollspy';
import KTScrollable from './components/scrollable';
import KTScrollto from './components/scrollto';
import KTSticky from './components/sticky';
import KTReparent from './components/reparent';
import KTToggle from './components/toggle';
import KTTooltip from './components/tooltip';
import KTStepper from './components/stepper';
import KTTheme from './components/theme';
import KTImageInput from './components/image-input';
import KTTogglePassword from './components/toggle-password';

export { default as KTUtils } from './helpers/utils';
export { default as KTDom } from './helpers/dom';
export { default as KTEventHandler } from './helpers/event-handler';
export { default as KTMenu } from './components/menu';
export { default as KTDropdown } from './components/dropdown';
export { default as KTModal } from './components/modal';
export { default as KTDrawer } from './components/drawer';
export { default as KTCollapse } from './components/collapse';
export { default as KTDismiss } from './components/dismiss';
export { default as KTTabs } from './components/scrollspy';
export { default as KTAccordion } from './components/accordion';
export { default as KTScrollspy } from './components/scrollspy';
export { default as KTScrollable } from './components/scrollable';
export { default as KTScrollto } from './components/scrollto';
export { default as KTSticky } from './components/sticky';
export { default as KTReparent } from './components/reparent';
export { default as KTToggle } from './components/toggle'
export { default as KTTooltip } from './components/tooltip';
export { default as KTStepper } from './components/stepper';
export { default as KTTheme } from './components/theme';
export { default as KTImageInput} from './components/image-input';
export { default as KTTogglePassword} from './components/toggle-password';

(globalThis as any).KTUtils = KTUtils;
(globalThis as any).KTDom = KTDom;
(globalThis as any).KTEventHandler = KTEventHandler;
(globalThis as any).KTMenu = KTMenu;
(globalThis as any).KTDropdown = KTDropdown;
(globalThis as any).KTModal = KTModal;
(globalThis as any).KTDrawer = KTDrawer;
(globalThis as any).KTCollapse = KTCollapse;
(globalThis as any).KTDismiss = KTDismiss;
(globalThis as any).KTTabs = KTTabs;
(globalThis as any).KTAccordion = KTAccordion;
(globalThis as any).KTScrollspy = KTScrollspy;
(globalThis as any).KTScrollable = KTScrollable;
(globalThis as any).KTScrollto = KTScrollto;
(globalThis as any).KTSticky = KTSticky;
(globalThis as any).KTReparent = KTReparent;
(globalThis as any).KTToggle = KTToggle;
(globalThis as any).KTTooltip = KTTooltip;
(globalThis as any).KTStepper = KTStepper;
(globalThis as any).KTTheme = KTTheme;
(globalThis as any).KTImageInput = KTImageInput;
(globalThis as any).KTTogglePassword = KTTogglePassword;

const KTComponents = {
  init(): void {
    KTMenu.init();
    KTDropdown.init();
    KTModal.init();
    KTDrawer.init();
    KTCollapse.init();
    KTDismiss.init();
    KTTabs.init();
    KTAccordion.init();
    KTScrollspy.init();
    KTScrollable.init();
    KTScrollto.init();
    KTSticky.init();
    KTReparent.init();
    KTToggle.init();
    KTTooltip.init();
    KTStepper.init();
    KTTheme.init();
    KTImageInput.init();
    KTTogglePassword.init();
  }
};

export default KTComponents;