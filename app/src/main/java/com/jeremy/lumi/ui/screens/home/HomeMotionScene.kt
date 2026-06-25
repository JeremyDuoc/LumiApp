package com.jeremy.lumi.ui.screens.home
val homeMotionSceneJson = """
{
  ConstraintSets: {
    start: {
      app_title: {
        width: 'wrap',
        height: 'wrap',
        top: ['parent', 'top', 16],
        start: ['parent', 'start', 24],
        alpha: 1
      },
      top_bar_date: {
        width: 'wrap',
        height: 'wrap',
        top: ['parent', 'top', 16],
        start: ['parent', 'start', 24],
        alpha: 0
      },
      top_bar_phase: {
        width: 'wrap',
        height: 'wrap',
        top: ['hero_ring_canvas', 'top'],
        bottom: ['hero_ring_canvas', 'bottom'],
        end: ['hero_ring_canvas', 'start', 12],
        alpha: 0
      },
      connections_icon: {
        width: 48,
        height: 48,
        top: ['parent', 'top', 16],
        end: ['parent', 'end', 8],
        alpha: 1
      },
      discreet_icon: {
        width: 48,
        height: 48,
        top: ['parent', 'top', 16],
        end: ['connections_icon', 'start', 0],
        alpha: 1
      },
      header_content: {
        width: 'spread',
        height: 'wrap',
        top: ['app_title', 'bottom', 4],
        start: ['parent', 'start'],
        end: ['parent', 'end'],
        alpha: 1
      },
      hero_card_bg: {
        width: 'spread',
        height: 'wrap',
        top: ['header_content', 'bottom', 16],
        start: ['parent', 'start', 24],
        end: ['parent', 'end', 24],
        alpha: 1
      },
      hero_ring_canvas: {
        width: 220,
        height: 220,
        top: ['hero_card_bg', 'top', 32],
        start: ['hero_card_bg', 'start'],
        end: ['hero_card_bg', 'end'],
        alpha: 1
      }
    },
    end: {
      app_title: {
        width: 'wrap',
        height: 'wrap',
        top: ['parent', 'top', 16],
        start: ['parent', 'start', 24],
        alpha: 0
      },
      top_bar_date: {
        width: 'wrap',
        height: 'wrap',
        top: ['parent', 'top', 16],
        bottom: ['connections_icon', 'bottom'],
        start: ['parent', 'start', 24],
        alpha: 1
      },
      top_bar_phase: {
        width: 'wrap',
        height: 'wrap',
        top: ['hero_ring_canvas', 'top'],
        bottom: ['hero_ring_canvas', 'bottom'],
        end: ['hero_ring_canvas', 'start', 12],
        alpha: 1
      },
      connections_icon: {
        width: 48,
        height: 48,
        top: ['parent', 'top', 16],
        end: ['parent', 'end', 8],
        alpha: 1
      },
      discreet_icon: {
        width: 48,
        height: 48,
        top: ['parent', 'top', 16],
        end: ['connections_icon', 'start', 0],
        alpha: 1
      },
      header_content: {
        width: 'spread',
        height: 'wrap',
        bottom: ['parent', 'top'],
        start: ['parent', 'start'],
        end: ['parent', 'end'],
        alpha: 0
      },
      hero_card_bg: {
        width: 'spread',
        height: 'wrap',
        bottom: ['parent', 'top'],
        start: ['parent', 'start', 24],
        end: ['parent', 'end', 24],
        alpha: 0
      },
      hero_ring_canvas: {
        width: 42,
        height: 42,
        top: ['parent', 'top', 16],
        bottom: ['connections_icon', 'bottom'],
        end: ['discreet_icon', 'start', 0],
        alpha: 1
      }
    }
  },
  Transitions: {
    default: {
      from: 'start',
      to: 'end',
      pathMotionArc: 'startVertical',
      KeyFrames: {
        KeyAttributes: [
          {
            target: ['header_content', 'hero_card_bg'],
            frames: [0, 50, 100],
            alpha: [1, 0.2, 0]
          },
          {
            target: ['app_title'],
            frames: [0, 40, 100],
            alpha: [1, 0, 0]
          },
          {
            target: ['top_bar_date', 'top_bar_phase'],
            frames: [0, 60, 100],
            alpha: [0, 0, 1]
          }
        ]
      }
    }
  }
}
"""
